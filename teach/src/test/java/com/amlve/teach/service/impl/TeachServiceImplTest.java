package com.amlve.teach.service.impl;

import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.amlve.examapi.dto.response.QuestionDetailResponse;
import com.amlve.examapi.service.ExamQuestionApi;
import com.amlve.teach.dto.request.HomeworkCloseRequest;
import com.amlve.teach.dto.request.HomeworkPublishRequest;
import com.amlve.teach.dto.response.HomeworkDetailResponse;
import com.amlve.teach.mapper.HomeworkMapper;
import com.amlve.teach.mapper.TeachHomeworkQuestionMapper;
import com.amlve.teach.tableEnt.Homework;
import com.amlve.teach.tableEnt.TeachHomeworkQuestion;
import com.amlve.teachapi.dto.request.StudentHomeworkQueryRequest;
import com.amlve.teachapi.dto.response.HomeworkResponse;
import com.amlve.teachapi.dto.response.HomeworkSubmitCheckResponse;
import com.amlve.teachapi.dto.response.StudentHomeworkBriefResponse;
import com.amlve.teachapi.dto.response.StudentHomeworkDetailResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TeachServiceImpl 单元测试。
 * 通过向 SecurityContextHolder 注入 UserIdAuthenticationToken 模拟登录教师，
 * 不依赖 JWT 过滤器，也无需修改任何公共模块代码。
 */
@ExtendWith(MockitoExtension.class)
class TeachServiceImplTest {

    @Mock
    private HomeworkMapper homeworkMapper;

    @Mock
    private TeachHomeworkQuestionMapper homeworkQuestionMapper;

    @Mock
    private ExamQuestionApi examQuestionApi;

    @InjectMocks
    private TeachServiceImpl teachService;

    private static final Long TEACHER_ID = 1L;

    @BeforeEach
    void setUpLoginTeacher() {
        // 模拟当前登录用户：userId=1 的教师（与运行时 JwtAuthenticationFilter 注入的结构一致）
        SecurityContextHolderTestUtil.loginAs(TEACHER_ID, "teacher");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolderTestUtil.logout();
    }

    private HomeworkPublishRequest buildRequest() {
        HomeworkPublishRequest request = new HomeworkPublishRequest();
        request.setTitle("第一次Java作业");
        request.setContent("完成教材第1章课后习题");
        request.setChapterId(1L);
        request.setDeadline(LocalDateTime.of(2026, 9, 15, 23, 59, 59));
        request.setClassId(10L);
        return request;
    }

    @Test
    void publishHomework_shouldWriteQuestionSnapshots_andSummarizeCountAndScore() {
        doAnswer(invocation -> {
            Homework h = invocation.getArgument(0);
            h.setId(100L);
            return 1;
        }).when(homeworkMapper).insert(any(Homework.class));

        HomeworkPublishRequest request = buildRequest();
        HomeworkPublishRequest.QuestionItem first = new HomeworkPublishRequest.QuestionItem();
        first.setQuestionId(11L);
        first.setScore(8);
        HomeworkPublishRequest.QuestionItem second = new HomeworkPublishRequest.QuestionItem();
        second.setQuestionId(12L);
        request.setQuestions(List.of(first, second));
        when(examQuestionApi.getQuestionDetail(11L)).thenReturn(Result.success(buildQuestionDetail(11L, 5)));
        when(examQuestionApi.getQuestionDetail(12L)).thenReturn(Result.success(buildQuestionDetail(12L, 5)));

        Long homeworkId = teachService.publishHomework(request);

        assertThat(homeworkId).isEqualTo(100L);
        // 两道题：第一题显式 8 分，第二题未传分 → 用题库默认 5 分
        ArgumentCaptor<Homework> homeworkCaptor = ArgumentCaptor.forClass(Homework.class);
        verify(homeworkMapper).insert(homeworkCaptor.capture());
        assertThat(homeworkCaptor.getValue().getQuestionCount()).isEqualTo(2);
        assertThat(homeworkCaptor.getValue().getTotalScore()).isEqualTo(13);

        // 题目快照：挂上作业ID、按入参顺序排序、选项/答案/知识点以 JSON 文本固化
        ArgumentCaptor<TeachHomeworkQuestion> snapshotCaptor = ArgumentCaptor.forClass(TeachHomeworkQuestion.class);
        verify(homeworkQuestionMapper, times(2)).insert(snapshotCaptor.capture());
        List<TeachHomeworkQuestion> snapshots = snapshotCaptor.getAllValues();
        TeachHomeworkQuestion saved = snapshots.get(0);
        assertThat(saved.getHomeworkId()).isEqualTo(100L);
        assertThat(saved.getQuestionId()).isEqualTo(11L);
        assertThat(saved.getSortOrder()).isEqualTo(1);
        assertThat(saved.getContent()).isEqualTo("题干-11");
        assertThat(saved.getType()).isEqualTo(1);
        assertThat(saved.getOptions()).isEqualTo("[\"A. 选项1\",\"B. 选项2\"]");
        assertThat(saved.getAnswer()).isEqualTo("\"B\"");
        assertThat(saved.getKnowledgePoints()).isEqualTo("[7,8]");
        // 快照存难度名称，不存配置ID（配置后续被停用/删除也不影响历史作业显示）
        assertThat(saved.getDifficulty()).isEqualTo("简单");
        assertThat(saved.getDefaultScore()).isEqualTo(5);
        assertThat(saved.getScore()).isEqualTo(8);
        assertThat(saved.getSourceType()).isEqualTo(3);
        assertThat(snapshots.get(1).getQuestionId()).isEqualTo(12L);
        assertThat(snapshots.get(1).getSortOrder()).isEqualTo(2);
        assertThat(snapshots.get(1).getScore()).isNull();
    }

    @Test
    void publishHomework_shouldRejectWithoutRecords_whenQuestionMissing() {
        HomeworkPublishRequest request = buildRequest();
        HomeworkPublishRequest.QuestionItem item = new HomeworkPublishRequest.QuestionItem();
        item.setQuestionId(999L);
        request.setQuestions(List.of(item));
        when(examQuestionApi.getQuestionDetail(999L)).thenReturn(Result.error("题目不存在"));

        assertThatThrownBy(() -> teachService.publishHomework(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("题目不存在");

        verify(homeworkMapper, never()).insert(any(Homework.class));
        verify(homeworkQuestionMapper, never()).insert(any(TeachHomeworkQuestion.class));
    }

    @Test
    void publishHomework_shouldReject_whenQuestionRepeated() {
        HomeworkPublishRequest request = buildRequest();
        HomeworkPublishRequest.QuestionItem item = new HomeworkPublishRequest.QuestionItem();
        item.setQuestionId(11L);
        request.setQuestions(List.of(item, item));

        assertThatThrownBy(() -> teachService.publishHomework(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能重复添加");

        verify(homeworkMapper, never()).insert(any(Homework.class));
        // 重复校验在查题库之前，不该触达 exam-api
        verify(examQuestionApi, never()).getQuestionDetail(any());
    }

    /** 构造题库题目详情（含选项、答案、知识点） */
    private QuestionDetailResponse buildQuestionDetail(Long id, int score) {
        QuestionDetailResponse detail = new QuestionDetailResponse();
        detail.setId(id);
        detail.setContent("题干-" + id);
        detail.setType(1);
        detail.setOptions(List.of("A. 选项1", "B. 选项2"));
        detail.setAnswer("B");
        detail.setAnalysis("解析-" + id);
        detail.setChapterId(1L);
        detail.setDifficultyConfigId(1L);
        detail.setDifficultyName("简单");
        detail.setScore(score);
        detail.setKnowledgePointIds(List.of(7L, 8L));
        return detail;
    }

    private Homework buildHomework(Long homeworkId) {
        Homework homework = new Homework();
        homework.setId(homeworkId);
        homework.setTitle("第一次Java作业");
        homework.setContent("完成教材第1章课后习题");
        homework.setChapterId(1L);
        homework.setTeacherId(TEACHER_ID);
        homework.setStatus(2);
        homework.setAllowLateSubmit(0);
        homework.setDeadline(LocalDateTime.of(2099, 12, 31, 23, 59, 59));
        return homework;
    }

    // ==================== 发布 ====================

    @Test
    void publishHomework_shouldMapFieldsAndInjectTeacherId() {
        // 模拟 MyBatis-Plus insert 主键回填
        doAnswer(invocation -> {
            Homework h = invocation.getArgument(0);
            h.setId(100L);
            return 1;
        }).when(homeworkMapper).insert(any(Homework.class));

        Long homeworkId = teachService.publishHomework(buildRequest());

        assertThat(homeworkId).isEqualTo(100L);
        // 验证落库实体字段映射完整，teacherId 从登录上下文注入
        ArgumentCaptor<Homework> captor = ArgumentCaptor.forClass(Homework.class);
        verify(homeworkMapper).insert(captor.capture());
        Homework saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("第一次Java作业");
        assertThat(saved.getChapterId()).isEqualTo(1L);
        assertThat(saved.getTeacherId()).isEqualTo(TEACHER_ID);
        assertThat(saved.getClassId()).isEqualTo(10L);
        assertThat(saved.getAllowLateSubmit()).isEqualTo(0);
        // 快照写入依赖 exam-api（协作申请中），题目数当前恒为 0
        assertThat(saved.getQuestionCount()).isZero();
        assertThat(saved.getPublishTime()).isNotNull();
        // 发布即已发布状态
        assertThat(saved.getStatus()).isEqualTo(2);
    }

    @Test
    void publishHomework_shouldReject_whenScopeMissing() {
        // 发布范围二选一：班级ID与年级都不传
        HomeworkPublishRequest request = buildRequest();
        request.setClassId(null);
        request.setGrade(null);
        assertThatThrownBy(() -> teachService.publishHomework(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("二选一");
        verify(homeworkMapper, never()).insert(any(Homework.class));
    }

    @Test
    void publishHomework_shouldReject_whenBothScopeProvided() {
        // 发布范围二选一：班级ID与年级同时传入
        HomeworkPublishRequest request = buildRequest();
        request.setGrade("2024级");
        assertThatThrownBy(() -> teachService.publishHomework(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("二选一");
        verify(homeworkMapper, never()).insert(any(Homework.class));
    }

    // ==================== 教师端查询 ====================

    @Test
    void listMyHomework_shouldConvertEntityToResponse() {
        Homework homework = buildHomework(100L);
        homework.setCreateTime(LocalDateTime.of(2026, 9, 7, 10, 0));
        when(homeworkMapper.selectList(any())).thenReturn(List.of(homework));

        List<com.amlve.teach.dto.response.HomeworkResponse> result = teachService.listMyHomework();

        assertThat(result).hasSize(1);
        com.amlve.teach.dto.response.HomeworkResponse response = result.get(0);
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getTitle()).isEqualTo("第一次Java作业");
    }

    @Test
    void getMyHomeworkDetail_shouldReturnSnapshotQuestions() {
        Homework homework = buildHomework(100L);
        when(homeworkMapper.selectById(100L)).thenReturn(homework);
        TeachHomeworkQuestion question = new TeachHomeworkQuestion();
        question.setId(501L);
        question.setQuestionId(9001L);
        question.setContent("Java 的基本数据类型有哪些？");
        question.setType(6);
        question.setAnswer("[\"8种\"]");
        question.setAnalysis("见教材第2章");
        question.setScore(20);
        when(homeworkQuestionMapper.selectList(any())).thenReturn(List.of(question));

        HomeworkDetailResponse detail = teachService.getMyHomeworkDetail(100L);

        assertThat(detail.getId()).isEqualTo(100L);
        assertThat(detail.getStatusName()).isEqualTo("已发布");
        assertThat(detail.getQuestions()).hasSize(1);
        HomeworkDetailResponse.QuestionItem item = detail.getQuestions().get(0);
        // 教师端可见答案与解析
        assertThat(item.getAnswer()).isEqualTo("[\"8种\"]");
        assertThat(item.getAnalysis()).isEqualTo("见教材第2章");
        assertThat(item.getTypeName()).isEqualTo("简答题");
    }

    @Test
    void getMyHomeworkDetail_shouldReject_whenNotOwner() {
        // 越权访问他人作业：按资源不存在处理，避免暴露作业存在性
        Homework other = buildHomework(100L);
        other.setTeacherId(999L);
        when(homeworkMapper.selectById(100L)).thenReturn(other);

        assertThatThrownBy(() -> teachService.getMyHomeworkDetail(100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }

    // ==================== 关闭 ====================

    @Test
    void closeHomework_shouldUpdateStatusAndCloseTime() {
        Homework homework = buildHomework(100L);
        when(homeworkMapper.selectById(100L)).thenReturn(homework);
        HomeworkCloseRequest request = new HomeworkCloseRequest();
        request.setHomeworkId(100L);
        request.setCloseReason("教学调整");

        teachService.closeHomework(request);

        ArgumentCaptor<Homework> captor = ArgumentCaptor.forClass(Homework.class);
        verify(homeworkMapper).updateById(captor.capture());
        Homework updated = captor.getValue();
        assertThat(updated.getStatus()).isEqualTo(3);
        assertThat(updated.getCloseTime()).isNotNull();
        assertThat(updated.getCloseReason()).isEqualTo("教学调整");
    }

    @Test
    void closeHomework_shouldReject_whenNotPublished() {
        // 状态机：仅已发布状态可关闭
        Homework homework = buildHomework(100L);
        homework.setStatus(3);
        when(homeworkMapper.selectById(100L)).thenReturn(homework);
        HomeworkCloseRequest request = new HomeworkCloseRequest();
        request.setHomeworkId(100L);

        assertThatThrownBy(() -> teachService.closeHomework(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅已发布");
        verify(homeworkMapper, never()).updateById(any(Homework.class));
    }

    // ==================== 跨模块接口（teach-api 契约） ====================

    @Test
    void getHomeworkById_shouldReturnDto_whenExists() {
        Homework homework = buildHomework(100L);
        when(homeworkMapper.selectById(100L)).thenReturn(homework);

        HomeworkResponse dto = teachService.getHomeworkById(100L).getData();

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getTitle()).isEqualTo("第一次Java作业");
    }

    @Test
    void getHomeworkById_shouldReturnNotFound_whenNotExists() {
        when(homeworkMapper.selectById(999L)).thenReturn(null);

        // 作业不存在：返回 NOT_FOUND 错误码而非 null，调用方通过 code 判断
        Result<HomeworkResponse> result = teachService.getHomeworkById(999L);
        assertThat(result.getCode()).isEqualTo(ErrorCodeConstant.NOT_FOUND);
        assertThat(result.getData()).isNull();
    }

    @Test
    void listHomeworkByTeacherId_shouldReturnDtoList() {
        Homework homework = buildHomework(100L);
        when(homeworkMapper.selectList(any())).thenReturn(List.of(homework));

        List<HomeworkResponse> dtoList = teachService.listHomeworkByTeacherId(TEACHER_ID).getData();

        assertThat(dtoList).hasSize(1);
        assertThat(dtoList.get(0).getId()).isEqualTo(100L);
    }

    @Test
    void listStudentHomework_shouldFilterByClassAndGrade() {
        Homework classHomework = buildHomework(100L);
        classHomework.setClassId(10L);
        Homework gradeHomework = buildHomework(101L);
        gradeHomework.setClassId(null);
        gradeHomework.setGrade("2024级");
        // 分页查询由 MyBatis-Plus 分页插件执行，单测 Mock 返回分页结果
        Page<Homework> page = new Page<>(1, 20);
        page.setRecords(List.of(classHomework, gradeHomework));
        when(homeworkMapper.selectPage(any(Page.class), any())).thenReturn(page);

        StudentHomeworkQueryRequest query = new StudentHomeworkQueryRequest();
        query.setClassId(10L);
        query.setGrade("2024级");
        Result<List<StudentHomeworkBriefResponse>> result = teachService.listStudentHomework(query);

        List<StudentHomeworkBriefResponse> list = result.getData();
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getHomeworkId()).isEqualTo(100L);
        assertThat(list.get(1).getHomeworkId()).isEqualTo(101L);
        // 章节名聚合依赖 knowledge-api（协作申请中），当前降级为空
        assertThat(list.get(0).getChapterName()).isNull();
    }

    @Test
    void getStudentHomeworkDetail_shouldHideAnswerAndParseOptions() {
        Homework homework = buildHomework(100L);
        when(homeworkMapper.selectById(100L)).thenReturn(homework);
        TeachHomeworkQuestion question = new TeachHomeworkQuestion();
        question.setId(501L);
        question.setContent("下列哪些是 Java 的基本数据类型？");
        question.setType(1);
        question.setOptions("[{\"key\":\"A\",\"value\":\"int\"}]");
        question.setAnswer("[\"A\"]");
        question.setAnalysis("见教材");
        question.setDefaultScore(10);
        question.setSortOrder(1);
        when(homeworkQuestionMapper.selectList(any())).thenReturn(List.of(question));

        StudentHomeworkDetailResponse detail = teachService.getStudentHomeworkDetail(100L).getData();

        assertThat(detail.getHomeworkId()).isEqualTo(100L);
        StudentHomeworkDetailResponse.StudentQuestionItem item = detail.getQuestions().get(0);
        assertThat(item.getTypeName()).isEqualTo("选择题");
        assertThat(item.getOptions()).hasSize(1);
        assertThat(item.getOptions().get(0).getKey()).isEqualTo("A");
        // score 为空时兜底取默认分值
        assertThat(item.getScore()).isEqualTo(10);
        // 学生端脱敏：确认 DTO 不含 answer/analysis 属性，答案无法从学生端读出
        List<String> fieldNames = java.util.Arrays.stream(
                        StudentHomeworkDetailResponse.StudentQuestionItem.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .toList();
        assertThat(fieldNames).doesNotContain("answer", "analysis");
    }

    @Test
    void getStudentHomeworkDetail_shouldReject_whenNotPublished() {
        // 学生端仅可查看已发布的作业
        Homework homework = buildHomework(100L);
        homework.setStatus(1);
        when(homeworkMapper.selectById(100L)).thenReturn(homework);

        Result<StudentHomeworkDetailResponse> result = teachService.getStudentHomeworkDetail(100L);
        assertThat(result.getCode()).isEqualTo(ErrorCodeConstant.BUSINESS_ERROR);
    }

    @Test
    void checkHomeworkSubmit_shouldPass_whenPublishedAndNotExpired() {
        Homework homework = buildHomework(100L);
        when(homeworkMapper.selectById(100L)).thenReturn(homework);

        HomeworkSubmitCheckResponse check = teachService.checkHomeworkSubmit(100L).getData();

        assertThat(check.getSubmittable()).isTrue();
        assertThat(check.getExpired()).isFalse();
        assertThat(check.getStatusName()).isEqualTo("已发布");
    }

    @Test
    void checkHomeworkSubmit_shouldReject_whenExpiredWithoutLateSubmit() {
        // 已截止且不允许迟交：不可提交
        Homework homework = buildHomework(100L);
        homework.setDeadline(LocalDateTime.now().minusDays(1));
        homework.setAllowLateSubmit(0);
        when(homeworkMapper.selectById(100L)).thenReturn(homework);

        HomeworkSubmitCheckResponse check = teachService.checkHomeworkSubmit(100L).getData();

        assertThat(check.getSubmittable()).isFalse();
        assertThat(check.getExpired()).isTrue();
        assertThat(check.getReason()).contains("截止");
    }

    @Test
    void checkHomeworkSubmit_shouldReject_whenClosed() {
        // 已截止（关闭）状态的作业不可提交
        Homework homework = buildHomework(100L);
        homework.setStatus(3);
        when(homeworkMapper.selectById(100L)).thenReturn(homework);

        HomeworkSubmitCheckResponse check = teachService.checkHomeworkSubmit(100L).getData();

        assertThat(check.getSubmittable()).isFalse();
        assertThat(check.getStatusName()).isEqualTo("已截止");
    }

    @Test
    void notifySubmit_shouldReturnSuccess_asDegraded() {
        // teach_homework 暂无 submitted_count 列，降级为记日志返回成功
        Result<Void> result = teachService.notifySubmit(100L);

        assertThat(result.getCode()).isEqualTo(200);
        verify(homeworkMapper, never()).updateById(any(Homework.class));
    }

    @Test
    void checkHomeworkSubmit_shouldReject_whenHomeworkMissing() {
        when(homeworkMapper.selectById(999L)).thenReturn(null);

        HomeworkSubmitCheckResponse check = teachService.checkHomeworkSubmit(999L).getData();

        assertThat(check.getSubmittable()).isFalse();
        assertThat(check.getReason()).isEqualTo("作业不存在");
    }

    @Test
    void getStudentHomeworkDetail_shouldReturnNotFound_whenMissing() {
        when(homeworkMapper.selectById(999L)).thenReturn(null);

        Result<StudentHomeworkDetailResponse> result = teachService.getStudentHomeworkDetail(999L);
        assertThat(result.getCode()).isEqualTo(ErrorCodeConstant.NOT_FOUND);
    }
}
