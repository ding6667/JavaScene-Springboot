package com.amlve.teach.service.impl;

import com.amlve.common.constant.ErrorMessageConstant;
import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.examapi.dto.response.QuestionDetailResponse;
import com.amlve.examapi.service.ExamQuestionApi;
import com.amlve.knowledgeapi.dto.response.ChapterNameDTO;
import com.amlve.knowledgeapi.service.KnowledgeChapterApi;
import com.amlve.teach.dto.request.HomeworkCloseRequest;
import com.amlve.teach.dto.request.HomeworkPublishRequest;
import com.amlve.teach.dto.response.HomeworkDetailResponse;
import com.amlve.teach.dto.response.StudentHomeworkBrief;
import com.amlve.teach.enums.HomeworkStatusEnum;
import com.amlve.teach.enums.QuestionTypeEnum;
import com.amlve.teach.mapper.HomeworkMapper;
import com.amlve.teach.mapper.TeachHomeworkQuestionMapper;
import com.amlve.teach.tableEnt.Homework;
import com.amlve.teach.tableEnt.TeachHomeworkQuestion;
import com.amlve.teach.service.TeachService;
import com.amlve.teachapi.service.TeachHomeworkService;
import com.amlve.teachapi.dto.request.StudentHomeworkQueryRequest;
import com.amlve.teachapi.dto.response.HomeworkQuestionSnapshotDTO;
import com.amlve.teachapi.dto.response.HomeworkResponse;
import com.amlve.teachapi.dto.response.HomeworkSubmitCheckResponse;
import com.amlve.teachapi.dto.response.StudentHomeworkBriefResponse;
import com.amlve.teachapi.dto.response.StudentHomeworkDetailResponse;
import com.amlve.systemapi.dto.api.UserBaseDTO;
import com.amlve.systemapi.service.UserApi;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @Resource
    private ExamQuestionApi examQuestionApi;

    /** 题目来源：3=人工录入（教师从题库手选） */
    private static final int SOURCE_TYPE_MANUAL = 3;

    // ===== 渲染配置（render_config）相关：与 exam 侧 QuestionRenderConfigCodec 保持同一套键名 =====
    /** 题型：1=选择 */
    private static final int QUESTION_TYPE_CHOICE = 1;
    /** 题型：2=填空 */
    private static final int QUESTION_TYPE_FILL = 2;
    private static final String RENDER_KEY_CHOICE_MODE = "choiceMode";
    private static final String RENDER_KEY_BLANK_COUNT = "blankCount";
    private static final String RENDER_CHOICE_SINGLE = "single";
    private static final String RENDER_CHOICE_MULTIPLE = "multiple";
    /** 题干空位：连续两个及以上下划线，或全角括号内留白 */
    private static final Pattern BLANK_PATTERN = Pattern.compile("_{2,}|[（(]\\s+[)）]");
    @Autowired
    private KnowledgeChapterApi knowledgeChapterApi;
    @Autowired
    private ObjectProvider<KnowledgeChapterApi>  knowledgeChapterApiProvider;
    /** 学生档案（班级/年级）查询：system 模块可选依赖，缺失时降级为仅校验作业状态 */
    @Autowired
    private ObjectProvider<UserApi> userApiProvider;

    // ==================== 教师端接口（TeachService） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publishHomework(HomeworkPublishRequest request) {
        // 发布范围二选一：classId（指定班级）或 grade（全年级），都不传或都传视为参数错误
        boolean hasClass = request.getClassId() != null;
        boolean hasGrade = request.getGrade() != null && !request.getGrade().isBlank();
        if (hasClass == hasGrade) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, ErrorMessageConstant.TEACH_SCOPE_MISSING);
        }
        // 选题在写库前一次性查好并校验（题目不存在、重复选题都在这里拦掉，避免半成品作业）
        List<TeachHomeworkQuestion> snapshots = buildQuestionSnapshots(request.getQuestions());

        Homework homework = new Homework();
        homework.setTitle(request.getTitle());
        homework.setContent(request.getContent());
        homework.setChapterId(request.getChapterId());
        homework.setDeadline(request.getDeadline());
        homework.setTeacherId(SecurityUtils.getUserId());
        homework.setClassId(request.getClassId());
        homework.setGrade(request.getGrade());
        // 迟交开关：入参未传时用默认值（0）
        homework.setAllowLateSubmit(request.getAllowLateSubmit() == null ? 0 : request.getAllowLateSubmit());
        // 题目数与总分以选题为准，未挂题时退回入参总分（默认 0）
        homework.setQuestionCount(snapshots.size());
        homework.setTotalScore(snapshots.isEmpty()
                ? (request.getTotalScore() == null ? 0 : request.getTotalScore())
                : snapshots.stream().mapToInt(this::effectiveScore).sum());
        // 发布即已发布状态（1=未发布，2=已发布）；createTime/updateTime 由 MetaHandler 自动填充
        homework.setStatus(HomeworkStatusEnum.PUBLISHED.getCode());
        homework.setPublishTime(LocalDateTime.now());
        homeworkMapper.insert(homework);

        // 题目快照挂上作业ID后逐条落库，与作业主表同事务
        for (TeachHomeworkQuestion snapshot : snapshots) {
            snapshot.setHomeworkId(homework.getId());
            homeworkQuestionMapper.insert(snapshot);
        }
        log.info("教师{}发布作业: {} (作业ID={}, 范围={}, 题目{}道, 总分{})",
                homework.getTeacherId(), homework.getTitle(), homework.getId(),
                hasClass ? "班级" + request.getClassId() : "年级" + request.getGrade(),
                homework.getQuestionCount(), homework.getTotalScore());
        return homework.getId();
    }

    /**
     * 按选题组装作业题目快照：题目内容/选项/答案/解析等从题库逐题固化，
     * 排序按入参数组顺序，来源未传时记为人工录入。
     *
     * @param items 选题列表，可空（空表示本次不挂题）
     * @return 未设置 homeworkId 的快照列表
     * @throws BusinessException 题目重复或题库中不存在时抛出
     */
    private List<TeachHomeworkQuestion> buildQuestionSnapshots(List<HomeworkPublishRequest.QuestionItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Set<Long> seen = new LinkedHashSet<>();
        for (HomeworkPublishRequest.QuestionItem item : items) {
            if (!seen.add(item.getQuestionId())) {
                throw new BusinessException(ErrorCodeConstant.PARAM_INVALID,
                        "同一道题不能重复添加：questionId=" + item.getQuestionId());
            }
        }
        List<TeachHomeworkQuestion> snapshots = new ArrayList<>(items.size());
        int sortOrder = 1;
        for (HomeworkPublishRequest.QuestionItem item : items) {
            Result<QuestionDetailResponse> detailResult = examQuestionApi.getQuestionDetail(item.getQuestionId());
            if (detailResult == null || detailResult.getCode() != ErrorCodeConstant.SUCCESS
                    || detailResult.getData() == null) {
                log.warn("HOMEWORK_QUESTION_NOT_FOUND 定位: {}", item.getQuestionId());
                throw new BusinessException(ErrorCodeConstant.PARAM_INVALID,
                        "题目不存在或不可用：questionId=" + item.getQuestionId());
            }
            QuestionDetailResponse detail = detailResult.getData();
            TeachHomeworkQuestion snapshot = new TeachHomeworkQuestion();
            snapshot.setQuestionId(item.getQuestionId());
            snapshot.setSourceType(item.getSourceType() == null ? SOURCE_TYPE_MANUAL : item.getSourceType());
            snapshot.setContent(detail.getContent());
            // 简短题目与渲染配置一并固化：作业详情/学生作答都读快照，不回题库取，避免题目被改后作业口径漂移
            snapshot.setShortTitle(detail.getShortTitle());
            snapshot.setRenderConfig(detail.getRenderConfig());
            snapshot.setType(detail.getType());
            snapshot.setOptions(toJsonText(detail.getOptions()));
            snapshot.setAnswer(toJsonText(detail.getAnswer()));
            snapshot.setAnalysis(detail.getAnalysis());
            // 作答与判题所需的其余字段一并固化：作业作答、错题归档、错题重做判题都只认这份快照
            snapshot.setCodeFramework(detail.getCodeFramework());
            snapshot.setRequirements(toJsonNode(detail.getRequirements()));
            snapshot.setHints(toJsonNode(detail.getHints()));
            snapshot.setSolutionOutline(detail.getSolutionOutline());
            snapshot.setTestCases(toJsonNode(detail.getTestCases()));
            snapshot.setJudgeMode(detail.getJudgeMode());
            snapshot.setDifficulty(detail.getDifficultyName());
            snapshot.setChapterId(detail.getChapterId());
            snapshot.setKnowledgePoints(toJsonText(detail.getKnowledgePointIds()));
            snapshot.setDefaultScore(detail.getScore());
            // 本题分值为空时留空，判分/统计时用 defaultScore 兜底
            snapshot.setScore(item.getScore());
            snapshot.setSortOrder(sortOrder++);
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    /** 快照的实际分值：显式分值优先，否则用题目默认分值 */
    private int effectiveScore(TeachHomeworkQuestion snapshot) {
        if (snapshot.getScore() != null) {
            return snapshot.getScore();
        }
        return snapshot.getDefaultScore() == null ? 0 : snapshot.getDefaultScore();
    }

    /** 快照 JSON 列统一以原始 JSON 文本存储，供前端自行解析 */
    private String toJsonText(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return JSON_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("作业题目快照序列化失败，置空: {}", value, e);
            return null;
        }
    }

    /**
     * 结构化对象/数组 → JsonNode，供带 JacksonTypeHandler 的 json 列直接落库。
     * 空集合统一落 null，避免把 {@code []} 存进去后前端还要区分「没配」和「配了空」。
     */
    private JsonNode toJsonNode(Object value) {
        if (value == null) {
            return null;
        }
        try {
            JsonNode node = JSON_MAPPER.valueToTree(value);
            return node == null || node.isNull()
                    || (node.isContainerNode() && node.isEmpty()) ? null : node;
        } catch (Exception e) {
            log.warn("作业题目快照结构化字段转换失败，置空: {}", value, e);
            return null;
        }
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

    // ==================== 学生端接口（仅 STUDENT 角色） ====================

    @Override
    public StudentHomeworkBrief getStudentHomeworkBrief(Long homeworkId) {
        Homework homework = requireStudentVisibleHomework(homeworkId);
        List<TeachHomeworkQuestion> snapshots = homeworkQuestionMapper.selectList(
                new LambdaQueryWrapper<TeachHomeworkQuestion>()
                        .select(TeachHomeworkQuestion::getId)
                        .eq(TeachHomeworkQuestion::getHomeworkId, homeworkId)
                        .orderByAsc(TeachHomeworkQuestion::getSortOrder)
                        .orderByAsc(TeachHomeworkQuestion::getId));

        StudentHomeworkBrief brief = new StudentHomeworkBrief();
        BeanUtils.copyProperties(homework, brief);
        brief.setHomeworkId(homework.getId());
        // 章节名跨模块取知识模块；模块未就绪或章节已删时置空，不阻塞学生作答
        brief.setChapterName(resolveChapterName(homework.getChapterId()));
        brief.setStatusName(HomeworkStatusEnum.nameOf(homework.getStatus()));
        // 题量以快照实际条数为准，前端按 questionIds 长度分页更可靠
        brief.setQuestionCount(snapshots.size());
        brief.setQuestionIds(snapshots.stream().map(TeachHomeworkQuestion::getId).toList());
        return brief;
    }

    @Override
    public StudentHomeworkDetailResponse.StudentQuestionItem getStudentHomeworkQuestion(Long questionId) {
        if (questionId == null) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, "作业题目ID不能为空");
        }
        TeachHomeworkQuestion snapshot = homeworkQuestionMapper.selectById(questionId);
        if (snapshot == null) {
            throw new BusinessException(ErrorCodeConstant.NOT_FOUND, ErrorMessageConstant.HOMEWORK_QUESTION_NOT_FOUND);
        }
        // 逐题接口同样校验所属作业可见，避免学生用 id 遍历到草稿/归档作业的题目
        requireStudentVisibleHomework(snapshot.getHomeworkId());
        return toStudentQuestionItem(snapshot);
    }

    /**
     * 学生端可见性校验：作业必须存在、处于「已发布/已截止」，且落在学生的班级或年级范围内。
     * 草稿与归档不下发。
     */
    private Homework requireStudentVisibleHomework(Long homeworkId) {
        if (homeworkId == null) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, "作业ID不能为空");
        }
        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) {
            log.warn("HOMEWORK_NOT_FOUND 定位: {}", homeworkId);
            throw new BusinessException(ErrorCodeConstant.NOT_FOUND, ErrorMessageConstant.HOMEWORK_NOT_FOUND);
        }
        boolean visible = Objects.equals(homework.getStatus(), HomeworkStatusEnum.PUBLISHED.getCode())
                || Objects.equals(homework.getStatus(), HomeworkStatusEnum.CLOSED.getCode());
        if (!visible) {
            throw new BusinessException(ErrorCodeConstant.BUSINESS_ERROR,
                    "作业不可查看，当前状态: " + HomeworkStatusEnum.nameOf(homework.getStatus()));
        }
        requireInStudentScope(homework);
        return homework;
    }

    /**
     * 作业发布范围校验（防止学生拿 id 遍历到别班作业）。
     * <p>
     * 指定班级的作业只对本班学生开放；{@code class_id} 为空的作业是对全年级发布的，要求年级一致。
     * 学生档案来自 system 模块，该模块未就绪（单模块编译/测试）时降级为跳过范围校验并打 warn，
     * 不因为依赖缺失把学生作答整体挡死。
     */
    private void requireInStudentScope(Homework homework) {
        UserApi userApi = userApiProvider.getIfAvailable();
        if (userApi == null) {
            log.warn("system 模块未就绪，跳过作业范围校验: homeworkId={}", homework.getId());
            return;
        }
        Long userId = SecurityUtils.getUserId();
        Result<UserBaseDTO> userResult = userApi.getUserById(userId);
        UserBaseDTO user = userResult == null || userResult.getData() == null ? null : userResult.getData();
        if (user == null) {
            log.warn("学生档案不存在，拒绝访问作业: userId={}, homeworkId={}", userId, homework.getId());
            throw new BusinessException(ErrorCodeConstant.BUSINESS_ERROR, "学生档案不存在，无法访问该作业");
        }
        if (homework.getClassId() != null) {
            if (!Objects.equals(homework.getClassId(), user.getClassId())) {
                log.warn("作业不在学生班级范围: userId={}, classId={}, homeworkClassId={}",
                        userId, user.getClassId(), homework.getClassId());
                throw new BusinessException(ErrorCodeConstant.ACCESS_DENIED, "作业不在你的班级范围内");
            }
            return;
        }
        String grade = user.getGrade();
        if (grade == null || grade.isBlank() || !grade.equals(homework.getGrade())) {
            log.warn("作业不在学生年级范围: userId={}, grade={}, homeworkGrade={}",
                    userId, grade, homework.getGrade());
            throw new BusinessException(ErrorCodeConstant.ACCESS_DENIED, "作业不在你的年级范围内");
        }
    }

    /**
     * 章节ID → 章节全称；知识模块未就绪、章节不存在或查询异常时返回 null（不阻塞作业作答）。
     */
    private String resolveChapterName(Long chapterId) {
        if (chapterId == null) {
            return null;
        }
        KnowledgeChapterApi api = knowledgeChapterApiProvider.getIfAvailable();
        if (api == null) {
            return null;
        }
        try {
            Result<ChapterNameDTO> result = api.getChapterNameById(chapterId);
            return result == null || result.getData() == null ? null : result.getData().getChapterName();
        } catch (Exception e) {
            log.warn("章节名查询失败: chapterId={}", chapterId, e);
            return null;
        }
    }

    /**
     * 快照实体 → 学生端题目视图：只给题干/选项/分值/渲染配置，
     * 不带 answer、analysis（脱敏在组装时做，不依赖 DTO 字段是否为 null）。
     */
    private StudentHomeworkDetailResponse.StudentQuestionItem toStudentQuestionItem(TeachHomeworkQuestion snapshot) {
        StudentHomeworkDetailResponse.StudentQuestionItem question = new StudentHomeworkDetailResponse.StudentQuestionItem();
        question.setId(snapshot.getId());
        // 快照ID与题库ID是两套主键：提交作业走题库ID，前端必须拿到才能回传
        question.setQuestionId(snapshot.getQuestionId());
        question.setContent(snapshot.getContent());
        question.setShortTitle(snapshot.getShortTitle());
        // 渲染配置：新快照已固化；老快照为空时按题型兜底，学生端始终拿得到 blankCount/choiceMode
        question.setRenderConfig(renderConfigOrDefault(snapshot));
        question.setType(snapshot.getType());
        question.setTypeName(QuestionTypeEnum.nameOf(snapshot.getType()));
        question.setOptions(parseOptions(snapshot.getOptions()));
        // 编程题没有代码框架就没法作答：老快照为空时前端会拿到 null，按空白编辑器处理
        question.setCodeFramework(snapshot.getCodeFramework());
        question.setRequirements(parseStringArray(snapshot.getRequirements()));
        question.setHints(parseStringArray(snapshot.getHints()));
        // 分值兜底：本题分值为空时取题目默认分值
        question.setScore(snapshot.getScore() != null ? snapshot.getScore() : snapshot.getDefaultScore());
        question.setSortOrder(snapshot.getSortOrder());
        return question;
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
        // 保持原契约：未传状态时只查已发布
        return Result.success(fetchStudentHomeworkPage(query,
                List.of(HomeworkStatusEnum.PUBLISHED.getCode())).getRecords());
    }

    @Override
    public Result<PageResult<StudentHomeworkBriefResponse>> pageStudentHomework(StudentHomeworkQueryRequest query) {
        // 学生可见范围：已发布 + 已截止；未传状态时按此范围过滤，草稿/归档不下发
        return Result.success(fetchStudentHomeworkPage(query,
                List.of(HomeworkStatusEnum.PUBLISHED.getCode(), HomeworkStatusEnum.CLOSED.getCode())));
    }

    /**
     * 学生作业分页查询（内部复用）：班级/年级范围、章节、截止时间过滤后分页，
     * 并把作业实体转换为去答案的简要视图。
     *
     * @param query           查询条件
     * @param defaultStatuses 未显式指定状态时使用的状态集合
     * @return 分页结果（records 为简要视图）
     */
    private PageResult<StudentHomeworkBriefResponse> fetchStudentHomeworkPage(
            StudentHomeworkQueryRequest query, List<Integer> defaultStatuses) {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize();

        // 班级与年级都缺失时无法界定可见范围；返回空而不是把全库已发布作业下发
        if (query.getClassId() == null && (query.getGrade() == null || query.getGrade().isBlank())) {
            log.warn("学生作业查询缺少班级与年级，返回空列表: status={}", query.getStatus());
            return PageResult.empty(pageNum, pageSize);
        }

        // 状态白名单：显式指定的 status 必须落在学生可见范围内，否则查不到数据
        // （否则前端传 status=1 就能翻出教师的草稿作业，造成越权）
        Integer requestedStatus = query.getStatus();
        if (requestedStatus != null && !defaultStatuses.contains(requestedStatus)) {
            log.warn("学生作业查询状态越界，返回空列表: status={}, allowed={}", requestedStatus, defaultStatuses);
            return PageResult.empty(pageNum, pageSize);
        }

        Page<Homework> page = homeworkMapper.selectPage(
                new Page<>(pageNum, pageSize), studentHomeworkWrapper(query, defaultStatuses));
        List<StudentHomeworkBriefResponse> list = page.getRecords().stream().map(homework -> {
            StudentHomeworkBriefResponse brief = new StudentHomeworkBriefResponse();
            BeanUtils.copyProperties(homework, brief);
            brief.setHomeworkId(homework.getId());
            brief.setStatusName(HomeworkStatusEnum.nameOf(homework.getStatus()));
            KnowledgeChapterApi ifAvailable = knowledgeChapterApiProvider.getIfAvailable();
            if (query.getChapterId() != null && ifAvailable != null) {
                brief.setChapterName(ifAvailable.getChapterNameById(query.getChapterId()).getData().getChapterName());
            }else
                brief.setChapterName(null);
            return brief;
        }).toList();
        return PageResult.of(page.getTotal(), pageNum, pageSize, list);
    }

    /**
     * 学生作业可见范围条件：状态 + (班级 或 年级) + 章节 + 是否未截止。
     */
    private LambdaQueryWrapper<Homework> studentHomeworkWrapper(
            StudentHomeworkQueryRequest query, List<Integer> defaultStatuses) {
        LambdaQueryWrapper<Homework> wrapper = new LambdaQueryWrapper<>();
        if (query.getStatus() != null) {
            wrapper.eq(Homework::getStatus, query.getStatus());
        } else {
            wrapper.in(Homework::getStatus, defaultStatuses);
        }
        // 范围匹配：指定班级的作业 + 该年级的全年级作业（class_id 为空且 grade 匹配）
        if (query.getClassId() != null) {
            String grade = query.getGrade();
            wrapper.and(w -> {
                w.eq(Homework::getClassId, query.getClassId());
                if (grade != null && !grade.isBlank()) {
                    w.or(sub -> sub.isNull(Homework::getClassId).eq(Homework::getGrade, grade));
                }
            });
        } else {
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
        return wrapper;
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
        response.setChapterName(resolveChapterName(homework.getChapterId()));
        response.setQuestions(loadSnapshotQuestions(homeworkId).stream()
                .map(item -> {
                    StudentHomeworkDetailResponse.StudentQuestionItem question = new StudentHomeworkDetailResponse.StudentQuestionItem();
                    question.setId(item.getId());
                    question.setQuestionId(item.getQuestionId());
                    question.setContent(item.getContent());
                    question.setShortTitle(item.getShortTitle());
                    // 渲染配置：新作业发布时已固化；老快照为空，这里按题型兜底，学生端始终拿得到 blankCount/choiceMode
                    question.setRenderConfig(renderConfigOrDefault(item));
                    question.setType(item.getType());
                    question.setTypeName(QuestionTypeEnum.nameOf(item.getType()));
                    question.setOptions(parseOptions(item.getOptions()));
                    // 编程题代码框架/作答要求/提示：发布时已固化，题库后续修改不影响本次作业
                    question.setCodeFramework(item.getCodeFramework());
                    question.setRequirements(parseStringArray(item.getRequirements()));
                    question.setHints(parseStringArray(item.getHints()));
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

    @Override
    public Result<List<HomeworkQuestionSnapshotDTO>> getQuestionSnapshots(List<Long> snapshotIds) {
        if (snapshotIds == null || snapshotIds.isEmpty()) {
            return Result.success(List.of());
        }
        List<Long> ids = snapshotIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Result.success(List.of());
        }
        List<TeachHomeworkQuestion> snapshots = homeworkQuestionMapper.selectBatchIds(ids);
        return Result.success(snapshots.stream().map(this::toSnapshotDTO).toList());
    }

    /**
     * 快照实体 → 判题载荷（含答案/用例，只给服务端）。
     * <p>
     * options/answer 在库里是原始 JSON 文本，这里按消费方需要转成结构化形态；
     * testCases 用 JsonNode 直接转，字段名与 exam 侧测试用例保持一致，避免二次映射。
     */
    private HomeworkQuestionSnapshotDTO toSnapshotDTO(TeachHomeworkQuestion snapshot) {
        HomeworkQuestionSnapshotDTO dto = new HomeworkQuestionSnapshotDTO();
        dto.setSnapshotId(snapshot.getId());
        dto.setQuestionId(snapshot.getQuestionId());
        dto.setContent(snapshot.getContent());
        dto.setShortTitle(snapshot.getShortTitle());
        dto.setType(snapshot.getType());
        dto.setOptions(parseOptionTexts(snapshot.getOptions()));
        dto.setRenderConfig(snapshot.getRenderConfig());
        dto.setCodeFramework(snapshot.getCodeFramework());
        dto.setRequirements(parseStringArray(snapshot.getRequirements()));
        dto.setHints(parseStringArray(snapshot.getHints()));
        dto.setAnswer(snapshot.getAnswer());
        dto.setAnalysis(snapshot.getAnalysis());
        dto.setSolutionOutline(snapshot.getSolutionOutline());
        if (snapshot.getTestCases() != null && snapshot.getTestCases().isArray()) {
            dto.setTestCases(JSON_MAPPER.convertValue(snapshot.getTestCases(),
                    JSON_MAPPER.getTypeFactory().constructCollectionType(
                            List.class, HomeworkQuestionSnapshotDTO.TestCaseItem.class)));
        }
        dto.setJudgeMode(snapshot.getJudgeMode());
        dto.setScore(snapshot.getScore() != null ? snapshot.getScore() : snapshot.getDefaultScore());
        dto.setChapterId(snapshot.getChapterId());
        dto.setKnowledgePointIds(parseLongArray(snapshot.getKnowledgePoints()));
        dto.setDifficultyName(snapshot.getDifficulty());
        return dto;
    }

    /** 知识点 json 数组文本 → ID 列表；解析失败返回 null（不阻塞错题归档） */
    private List<Long> parseLongArray(String jsonArray) {
        if (jsonArray == null || jsonArray.isBlank()) {
            return null;
        }
        try {
            return JSON_MAPPER.readValue(jsonArray, new TypeReference<List<Long>>() {
            });
        } catch (Exception e) {
            log.warn("知识点快照解析失败，置空: {}", e.getMessage());
            return null;
        }
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
            // 老快照没有 render_config，按题型兜底，教师端预览与学生端渲染口径一致
            item.setRenderConfig(renderConfigOrDefault(q));
            return item;
        }).toList();
    }

    /**
     * 解析选项 JSON 为前端可用的键值列表。
     * <p>
     * 题库里 options 是纯字符串数组（如 {@code ["A. xx","B. yy"]}），并不是 key/value 对象；
     * 早期数据也可能是 [{@code {"key":"A","value":"xx"}}]。两种都吃：
     * 字符串项统一补上 A/B/C… 键，并去掉题干里自带的「A. 」前缀，避免前端自己再猜一遍。
     * 解析失败返回 null（前端按无选项处理）。
     */
    private List<StudentHomeworkDetailResponse.OptionItem> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return null;
        }
        try {
            JsonNode node = JSON_MAPPER.readTree(optionsJson);
            if (node == null || !node.isArray() || node.isEmpty()) {
                return null;
            }
            List<StudentHomeworkDetailResponse.OptionItem> items = new ArrayList<>(node.size());
            int index = 0;
            for (JsonNode element : node) {
                if (element == null || element.isNull()) {
                    continue;
                }
                StudentHomeworkDetailResponse.OptionItem item =
                        new StudentHomeworkDetailResponse.OptionItem();
                if (element.isObject()) {
                    item.setKey(textOf(element.get("key"), defaultOptionKey(index)));
                    item.setValue(textOf(element.get("value"), element.toString()));
                } else {
                    item.setKey(defaultOptionKey(index));
                    item.setValue(stripOptionPrefix(element.asText()));
                }
                index++;
                items.add(item);
            }
            return items.isEmpty() ? null : items;
        } catch (Exception e) {
            log.warn("题目选项解析失败，降级为空: {}", e.getMessage());
            return null;
        }
    }

    /** 选项纯文本数组（作答要求/判题载荷用），兼容 key/value 对象格式 */
    private List<String> parseOptionTexts(String optionsJson) {
        List<StudentHomeworkDetailResponse.OptionItem> items = parseOptions(optionsJson);
        if (items == null) {
            return null;
        }
        return items.stream().map(StudentHomeworkDetailResponse.OptionItem::getValue).toList();
    }

    /** 选项默认键：A、B、C…；超过 26 项退化为数字下标 */
    private String defaultOptionKey(int index) {
        return index < 26 ? String.valueOf((char) ('A' + index)) : String.valueOf(index + 1);
    }

    /** 去掉选项文本自带的「A. 」「A、」「A)」前缀，键位由后端统一下发 */
    private String stripOptionPrefix(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.replaceFirst("^\\s*[A-Za-z]\\s*[.、．)）:：]\\s*", "");
    }

    private String textOf(JsonNode node, String fallback) {
        return node == null || node.isNull() || node.asText().isBlank() ? fallback : node.asText();
    }

    /**
     * json 数组列（requirements / hints）→ String 列表；非数组或解析失败返回 null，
     * 由前端按「没有配置」处理（空数组与未配置同义，不额外区分）。
     */
    private List<String> parseStringArray(JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            return null;
        }
        List<String> values = new ArrayList<>(node.size());
        for (JsonNode element : node) {
            if (element != null && !element.isNull()) {
                values.add(element.asText());
            }
        }
        return values.isEmpty() ? null : values;
    }

    /**
     * 渲染配置兜底：本次改造前发布的作业快照没有 render_config，
     * 这里按题型给最小可用值（填空按题干空位定空数、选择按答案个数定单选/多选），
     * 保证学生端任何时候都能按 renderConfig 渲染，不必再自行判定。
     */
    private JsonNode renderConfigOrDefault(TeachHomeworkQuestion question) {
        return renderConfigOrDefault(question.getRenderConfig(), question.getType(),
                question.getContent(), question.getAnswer());
    }

    /**
     * 渲染配置兜底（教师端快照视图版）：学生端详情是从教师端视图二次转换出来的，
     * 同一次请求里已经兜过一次，这里直接复用视图上的值。
     */
    private JsonNode renderConfigOrDefault(HomeworkDetailResponse.QuestionItem item) {
        return renderConfigOrDefault(item.getRenderConfig(), item.getType(),
                item.getContent(), item.getAnswer());
    }

    /**
     * 渲染配置兜底核心逻辑：配置优先，缺失时按题型给最小可用值
     * （填空按题干空位定空数、选择按答案个数定单选/多选），其余题型原样返回。
     */
    private JsonNode renderConfigOrDefault(JsonNode configured, Integer type, String content, String answerJson) {
        if (configured != null && !configured.isEmpty()) {
            return configured;
        }
        if (type == null) {
            return configured;
        }
        if (type == QUESTION_TYPE_FILL) {
            int blanks = countBlanks(content);
            return JSON_MAPPER.createObjectNode().put(RENDER_KEY_BLANK_COUNT, Math.max(blanks, 1));
        }
        if (type == QUESTION_TYPE_CHOICE) {
            return JSON_MAPPER.createObjectNode().put(RENDER_KEY_CHOICE_MODE,
                    choiceCountOf(answerJson) > 1 ? RENDER_CHOICE_MULTIPLE : RENDER_CHOICE_SINGLE);
        }
        return configured;
    }

    /** 题干空位数：连续两个及以上下划线，或括号内留白 */
    private int countBlanks(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        Matcher matcher = BLANK_PATTERN.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /** 答案个数：快照里 answer 是 JSON 数组字符串，解析失败按 1 处理 */
    private int choiceCountOf(String answerJson) {
        if (answerJson == null || answerJson.isBlank()) {
            return 0;
        }
        try {
            JsonNode node = JSON_MAPPER.readTree(answerJson);
            if (node != null && node.isArray()) {
                return node.size();
            }
        } catch (Exception e) {
            log.debug("快照标准答案解析失败，按单选取值: {}", answerJson);
        }
        return 1;
    }

    /** options JSON 解析用 Jackson 映射器（无 hutool 依赖，用模块内已有 Jackson） */
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private HomeworkResponse toDTO(Homework homework) {
        HomeworkResponse dto = new HomeworkResponse();
        BeanUtils.copyProperties(homework, dto);
        return dto;
    }
}
