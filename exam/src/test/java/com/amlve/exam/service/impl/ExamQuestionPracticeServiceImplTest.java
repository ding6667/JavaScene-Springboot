package com.amlve.exam.service.impl;

import com.amlve.common.request.PageResult;
import com.amlve.exam.enumeration.QuestionDifficultyEnum;
import com.amlve.common.enums.QuestionTypeEnum;
import com.amlve.exam.mapper.ExamQuestionMapper;
import com.amlve.exam.tableEnt.ExamQuestion;
import com.amlve.examapi.dto.request.PracticeQueryRequest;
import com.amlve.examapi.dto.response.PracticeDetailVO;
import com.amlve.examapi.dto.response.PracticeItemVO;
import com.amlve.studyapi.service.StudyPracticeService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * {@link ExamQuestionPracticeServiceImpl} 单元测试。
 * <p>
 * study 侧个人状态实现缺失时，用 ObjectProvider 返回 null 验证空值兜底。
 */
@ExtendWith(MockitoExtension.class)
class ExamQuestionPracticeServiceImplTest {

    @Mock
    private ExamQuestionMapper questionMapper;

    @Mock
    private ObjectProvider<StudyPracticeService> studyPracticeServiceProvider;

    @InjectMocks
    private ExamQuestionPracticeServiceImpl practiceService;

    @Test
    void listPractice_mapsCommonStats_withoutPersonalStatus() {
        ExamQuestion question = new ExamQuestion();
        question.setId(1L);
        question.setContent("题干");
        question.setType(QuestionTypeEnum.CHOICE);
        question.setOptions("[\"A\",\"B\"]");
        question.setChapterId(5L);
        question.setDifficulty(QuestionDifficultyEnum.BASIC);
        question.setScore(5);
        question.setAcceptanceRate(new BigDecimal("66.67"));
        question.setSubmitCount(9);

        Page<ExamQuestion> page = new Page<>(1, 10);
        page.setRecords(List.of(question));
        page.setTotal(1);
        doReturn(page).when(questionMapper).selectPage(any(), any());
        when(studyPracticeServiceProvider.getIfAvailable()).thenReturn(null);

        PageResult<PracticeItemVO> result = practiceService.listPractice(1L, new PracticeQueryRequest());

        assertThat(result.getRecords()).hasSize(1);
        PracticeItemVO vo = result.getRecords().get(0);
        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getOptions()).containsExactly("A", "B");
        assertThat(vo.getAcceptanceRate()).isEqualTo(66.67);
        assertThat(vo.getSubmitCount()).isEqualTo(9);
        assertThat(vo.getFavorited()).isNull();
        assertThat(vo.getDone()).isNull();
    }

    @Test
    void getPracticeDetail_returnsOnlySampleCases() {
        ExamQuestion question = new ExamQuestion();
        question.setId(1L);
        question.setContent("编程题");
        question.setType(QuestionTypeEnum.PROGRAMMING);
        question.setChapterId(5L);
        question.setAcceptanceRate(new BigDecimal("50.00"));
        question.setTestCases(List.of(
                buildCase("1", "1", true, "示例"),
                buildCase("2", "2", false, "隐藏")));
        when(questionMapper.selectById(1L)).thenReturn(question);
        when(studyPracticeServiceProvider.getIfAvailable()).thenReturn(null);

        PracticeDetailVO vo = practiceService.getPracticeDetail(1L);

        assertThat(vo.getId()).isEqualTo(1L);
        assertThat(vo.getSampleTestCases()).hasSize(1);
        assertThat(vo.getSampleTestCases().get(0).getInput()).isEqualTo("1");
        assertThat(vo.getAcceptanceRate()).isEqualTo(50.00);
    }

    private ExamQuestion.TestCase buildCase(String input, String output, boolean sample, String desc) {
        ExamQuestion.TestCase testCase = new ExamQuestion.TestCase();
        testCase.setInput(input);
        testCase.setExpectedOutput(output);
        testCase.setIsSample(sample);
        testCase.setDescription(desc);
        return testCase;
    }
}
