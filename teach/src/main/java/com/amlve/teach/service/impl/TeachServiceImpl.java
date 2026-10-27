package com.amlve.teach.service.impl;

import com.amlve.common.constant.ErrorMessageConstant;
import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.teach.dto.request.HomeworkCloseRequest;
import com.amlve.teach.dto.request.HomeworkPublishRequest;
import com.amlve.teach.dto.response.HomeworkDetailResponse;
import com.amlve.teach.enums.HomeworkStatusEnum;
import com.amlve.teach.enums.QuestionTypeEnum;
import com.amlve.teach.mapper.HomeworkMapper;
import com.amlve.teach.mapper.TeachHomeworkQuestionMapper;
import com.amlve.teach.tableEnt.Homework;
import com.amlve.teach.tableEnt.TeachHomeworkQuestion;
import com.amlve.teach.service.TeachService;
import com.amlve.teachapi.service.TeachHomeworkService;
import com.amlve.teachapi.dto.request.StudentHomeworkQueryRequest;
import com.amlve.teachapi.dto.response.HomeworkResponse;
import com.amlve.teachapi.dto.response.HomeworkSubmitCheckResponse;
import com.amlve.teachapi.dto.response.StudentHomeworkBriefResponse;
import com.amlve.teachapi.dto.response.StudentHomeworkDetailResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 教师教学业务实现，同时实现 teach-api 对外接口供其他模块调用
 */
@Service
@Slf4j
public class TeachServiceImpl implements TeachService, TeachHomeworkService {

    @Resource
    private HomeworkMapper homeworkMapper;

    @Resource
    private TeachHomeworkQuestionMapper homeworkQuestionMapper;

    // ==================== 教师端接口（TeachService） ====================

    @Override
    public Long publishHomework(HomeworkPublishRequest request) {
        // 发布范围二选一：classId（指定班级）或 grade（全年级），都不传或都传视为参数错误
        boolean hasClass = request.getClassId() != null;
        boolean hasGrade = request.getGrade() != null && !request.getGrade().isBlank();
        if (hasClass == hasGrade) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, ErrorMessageConstant.TEACH_SCOPE_MISSING);
        }
        Homework homework = new Homework();
        homework.setTitle(request.getTitle());
        homework.setContent(request.getContent());
        homework.setChapterId(request.getChapterId());
        homework.setDeadline(request.getDeadline());
        homework.setTeacherId(SecurityUtils.getUserId());
        homework.setClassId(request.getClassId());
        homework.setGrade(request.getGrade());
        // 迟交开关与总分：入参未传时用默认值（0）
        homework.setAllowLateSubmit(request.getAllowLateSubmit() == null ? 0 : request.getAllowLateSubmit());
        homework.setTotalScore(request.getTotalScore() == null ? 0 : request.getTotalScore());
        // 题目快照写入依赖 exam-api 题目查询接口（协作申请中），当前恒为 0
        homework.setQuestionCount(0);
        // 发布即已发布状态（1=未发布，2=已发布）；createTime/updateTime 由 MetaHandler 自动填充
        homework.setStatus(2);
        homework.setPublishTime(LocalDateTime.now());
        homeworkMapper.insert(homework);
        log.info("教师{}发布作业: {} (作业ID={}, 范围={})",
                homework.getTeacherId(), homework.getTitle(), homework.getId(),
                hasClass ? "班级" + request.getClassId() : "年级" + request.getGrade());
        return homework.getId();
    }

    @Override
    public List<com.amlve.teach.dto.response.HomeworkResponse> listMyHomework() {
        Long teacherId = SecurityUtils.getUserId();
        return homeworkMapper.selectList(
                        new LambdaQueryWrapper<Homework>()
                                .eq(Homework::getTeacherId, teacherId)
                                .orderByDesc(Homework::getCreateTime))
                .stream().map(homework -> {
                    com.amlve.teach.dto.response.HomeworkResponse response = new com.amlve.teach.dto.response.HomeworkResponse();
                    BeanUtils.copyProperties(homework, response);
                    return response;
                }).toList();
    }

    @Override
    public HomeworkDetailResponse getMyHomeworkDetail(Long homeworkId) {
        Homework homework = requireOwnedHomework(homeworkId);
        HomeworkDetailResponse response = new HomeworkDetailResponse();
        BeanUtils.copyProperties(homework, response);
        response.setStatusName(HomeworkStatusEnum.nameOf(homework.getStatus()));
        response.setQuestions(loadSnapshotQuestions(homeworkId));
        return response;
    }

    @Override
    public void closeHomework(HomeworkCloseRequest request) {
        Homework homework = requireOwnedHomework(request.getHomeworkId());
        // 状态机：仅已发布状态可关闭
        if (!Objects.equals(homework.getStatus(), HomeworkStatusEnum.PUBLISHED.getCode())) {
            throw new BusinessException(ErrorCodeConstant.BUSINESS_ERROR,
                    "仅已发布状态的作业可关闭，当前状态: " + HomeworkStatusEnum.nameOf(homework.getStatus()));
        }
        Homework update = new Homework();
        update.setId(homework.getId());
        update.setStatus(HomeworkStatusEnum.CLOSED.getCode());
        update.setCloseTime(LocalDateTime.now());
        update.setCloseReason(request.getCloseReason());
        homeworkMapper.updateById(update);
        log.info("教师{}关闭作业: 作业ID={}, 原因={}",
                SecurityUtils.getUserId(), homework.getId(), request.getCloseReason());
    }

    // ==================== 跨模块接口（TeachHomeworkService，teach-api 契约） ====================

    @Override
    public Result<HomeworkResponse> getHomeworkById(Long homeworkId) {
        Homework homework = homeworkMapper.selectById(homeworkId);
        // 作业不存在时返回 NOT_FOUND 错误码，调用方通过 code 判断而非判空
        if (homework == null) {
            log.warn("HOMEWORK_NOT_FOUND 定位: {}", homeworkId);
            return Result.error(ErrorCodeConstant.NOT_FOUND, ErrorMessageConstant.HOMEWORK_NOT_FOUND);
        }
        return Result.success(toDTO(homework));
    }

    @Override
    public Result<List<HomeworkResponse>> listHomeworkByTeacherId(Long teacherId) {
        // 教师无已发布作业返回空列表，属正常业务结果，不返回 error
        return Result.success(homeworkMapper.selectList(
                        new LambdaQueryWrapper<Homework>()
                                .eq(Homework::getTeacherId, teacherId)
                                .orderByDesc(Homework::getCreateTime))
                .stream().map(this::toDTO).toList());
    }

    @Override
    public Result<List<StudentHomeworkBriefResponse>> listStudentHomework(StudentHomeworkQueryRequest query) {
        LambdaQueryWrapper<Homework> wrapper = new LambdaQueryWrapper<Homework>()
                // 默认只查已发布（契约默认值 2）
                .eq(Homework::getStatus, query.getStatus() == null ? 2 : query.getStatus());
        // 范围匹配：指定班级的作业 + 该年级的全年级作业（class_id 为空且 grade 匹配）
        if (query.getClassId() != null) {
            String grade = query.getGrade();
            wrapper.and(w -> {
                w.eq(Homework::getClassId, query.getClassId());
                if (grade != null && !grade.isBlank()) {
                    w.or(sub -> sub.isNull(Homework::getClassId).eq(Homework::getGrade, grade));
                }
            });
        } else if (query.getGrade() != null && !query.getGrade().isBlank()) {
            wrapper.isNull(Homework::getClassId).eq(Homework::getGrade, query.getGrade());
        }
        if (query.getChapterId() != null) {
            wrapper.eq(Homework::getChapterId, query.getChapterId());
        }
        // 只查未截止：deadline 晚于当前时间
        if (Boolean.TRUE.equals(query.getOnlyNotExpired())) {
            wrapper.gt(Homework::getDeadline, LocalDateTime.now());
        }
        wrapper.orderByDesc(Homework::getPublishTime);

        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize();
        List<Homework> records = homeworkMapper.selectPage(new Page<>(pageNum, pageSize), wrapper).getRecords();

        List<StudentHomeworkBriefResponse> list = records.stream().map(homework -> {
            StudentHomeworkBriefResponse brief = new StudentHomeworkBriefResponse();
            BeanUtils.copyProperties(homework, brief);
            brief.setHomeworkId(homework.getId());
            // 章节名聚合依赖 knowledge-api 章节查询接口（协作申请中），当前降级为空
            brief.setChapterName(null);
            return brief;
        }).toList();
        return Result.success(list);
    }

    @Override
    public Result<StudentHomeworkDetailResponse> getStudentHomeworkDetail(Long homeworkId) {
        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) {
            log.warn("HOMEWORK_NOT_FOUND 定位: {}", homeworkId);
            return Result.error(ErrorCodeConstant.NOT_FOUND, ErrorMessageConstant.HOMEWORK_NOT_FOUND);
        }
        // 学生端仅可查看已发布的作业
        if (!Objects.equals(homework.getStatus(), HomeworkStatusEnum.PUBLISHED.getCode())) {
            return Result.error(ErrorCodeConstant.BUSINESS_ERROR,
                    "作业不可查看，当前状态: " + HomeworkStatusEnum.nameOf(homework.getStatus()));
        }
        StudentHomeworkDetailResponse response = new StudentHomeworkDetailResponse();
        BeanUtils.copyProperties(homework, response);
        response.setHomeworkId(homework.getId());
        response.setChapterName(null);
        response.setQuestions(loadSnapshotQuestions(homeworkId).stream()
                .map(item -> {
                    StudentHomeworkDetailResponse.StudentQuestionItem question = new StudentHomeworkDetailResponse.StudentQuestionItem();
                    question.setId(item.getId());
                    question.setContent(item.getContent());
                    question.setType(item.getType());
                    question.setTypeName(QuestionTypeEnum.nameOf(item.getType()));
                    question.setOptions(parseOptions(item.getOptions()));
                    // 分值兜底：score 为空时取题目默认分值
                    question.setScore(item.getScore() != null ? item.getScore() : item.getDefaultScore());
                    question.setSortOrder(item.getSortOrder());
                    // 学生端脱敏：不返回 answer/analysis
                    return question;
                }).toList());
        return Result.success(response);
    }

    @Override
    public Result<HomeworkSubmitCheckResponse> checkHomeworkSubmit(Long homeworkId) {
        Homework homework = homeworkMapper.selectById(homeworkId);
        HomeworkSubmitCheckResponse response = new HomeworkSubmitCheckResponse();
        response.setHomeworkId(homeworkId);
        if (homework == null) {
            response.setSubmittable(false);
            response.setExpired(false);
            response.setReason("作业不存在");
            return Result.success(response);
        }
        response.setStatus(homework.getStatus());
        response.setStatusName(HomeworkStatusEnum.nameOf(homework.getStatus()));
        response.setDeadline(homework.getDeadline());
        response.setAllowLateSubmit(homework.getAllowLateSubmit());
        // 截止判定：当前时间晚于 deadline 即视为过期
        boolean expired = homework.getDeadline() != null && LocalDateTime.now().isAfter(homework.getDeadline());
        response.setExpired(expired);
        if (!Objects.equals(homework.getStatus(), HomeworkStatusEnum.PUBLISHED.getCode())) {
            response.setSubmittable(false);
            response.setReason("作业当前状态为[" + HomeworkStatusEnum.nameOf(homework.getStatus()) + "]，不可提交");
        } else if (expired && !Objects.equals(homework.getAllowLateSubmit(), 1)) {
            response.setSubmittable(false);
            response.setReason("作业已截止，不允许迟交");
        } else {
            response.setSubmittable(true);
        }
        return Result.success(response);
    }

    @Override
    public Result<Void> notifySubmit(Long homeworkId) {
        // teach_homework 表暂无 submitted_count 列（已向 sql 负责人发起加列申请），
        // 当前降级为仅记录提交回调日志，待列就绪后补 UPDATE submitted_count = submitted_count + 1
        log.info("收到作业提交通知: 作业ID={}（提交人数计数待表结构支持）", homeworkId);
        return Result.success(null);
    }

    // ==================== 私有方法 ====================

    /**
     * 按作业ID查询并校验归属：必须存在且属于当前登录教师，否则抛业务异常
     */
    private Homework requireOwnedHomework(Long homeworkId) {
        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) {
            log.warn("HOMEWORK_NOT_FOUND 定位: {}", homeworkId);
            throw new BusinessException(ErrorCodeConstant.NOT_FOUND, ErrorMessageConstant.HOMEWORK_NOT_FOUND);
        }
        Long teacherId = SecurityUtils.getUserId();
        if (!Objects.equals(homework.getTeacherId(), teacherId)) {
            // 越权访问他人作业按资源不存在处理，避免暴露作业存在性
            log.warn("教师{}尝试访问非本人作业: 作业ID={}, 归属教师={}", teacherId, homeworkId, homework.getTeacherId());
            log.warn("HOMEWORK_NOT_FOUND 定位: {}", homeworkId);
            throw new BusinessException(ErrorCodeConstant.NOT_FOUND, ErrorMessageConstant.HOMEWORK_NOT_FOUND);
        }
        return homework;
    }

    /**
     * 加载作业题目快照列表（按 sortOrder 升序），教师端视角（含答案解析）
     */
    private List<HomeworkDetailResponse.QuestionItem> loadSnapshotQuestions(Long homeworkId) {
        List<TeachHomeworkQuestion> questions = homeworkQuestionMapper.selectList(
                new LambdaQueryWrapper<TeachHomeworkQuestion>()
                        .eq(TeachHomeworkQuestion::getHomeworkId, homeworkId)
                        .orderByAsc(TeachHomeworkQuestion::getSortOrder));
        return questions.stream().map(q -> {
            HomeworkDetailResponse.QuestionItem item = new HomeworkDetailResponse.QuestionItem();
            BeanUtils.copyProperties(q, item);
            item.setTypeName(QuestionTypeEnum.nameOf(q.getType()));
            return item;
        }).toList();
    }

    /**
     * 解析选项 JSON 为前端可用的键值列表，格式异常时降级返回 null
     */
    private List<StudentHomeworkDetailResponse.OptionItem> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return null;
        }
        try {
            // Jackson 线程安全，静态单例即可；仅解析键值对结构
            return JSON_MAPPER.readValue(optionsJson,
                    new TypeReference<List<StudentHomeworkDetailResponse.OptionItem>>() {
                    });
        } catch (Exception e) {
            log.warn("题目选项解析失败，降级为空: {}", e.getMessage());
            return null;
        }
    }

    /** options JSON 解析用 Jackson 映射器（无 hutool 依赖，用模块内已有 Jackson） */
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private HomeworkResponse toDTO(Homework homework) {
        HomeworkResponse dto = new HomeworkResponse();
        BeanUtils.copyProperties(homework, dto);
        return dto;
    }
}
