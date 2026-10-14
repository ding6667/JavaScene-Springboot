package com.amlve.exam.service.impl;

import com.amlve.common.request.Result;
import com.amlve.exam.enumeration.QuestionDifficultyEnum;
import com.amlve.common.enums.QuestionTypeEnum;
import com.amlve.exam.mapper.ExamQuestionMapper;
import com.amlve.exam.tableEnt.ExamQuestion;
import com.amlve.examapi.dto.response.QuestionBaseResponse;
import com.amlve.examapi.dto.response.QuestionDetailResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link ExamQuestionApiImpl} 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class ExamQuestionApiImplTest {

    @Mock
    private ExamQuestionMapper questionMapper;

    @InjectMocks
    private ExamQuestionApiImpl examQuestionApi;

    @Test
    void getQuestionDetail_returnsMappedDetail() {
        ExamQuestion question = new ExamQuestion();
        question.setId(1L);
        question.setContent("题干");
        question.setType(QuestionTypeEnum.CHOICE);
        question.setOptions("[\"A\",\"B\"]");
        question.setAnswer("A");
        question.setAnalysis("解析");
        question.setChapterId(5L);
        question.setDifficulty(QuestionDifficultyEnum.BASIC);
        question.setScore(5);
        question.setJudgeMode(1);
        when(questionMapper.selectById(1L)).thenReturn(question);

        Result<QuestionDetailResponse> result = examQuestionApi.getQuestionDetail(1L);

        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getId()).isEqualTo(1L);
        assertThat(result.getData().getType()).isEqualTo(1);
        assertThat(result.getData().getOptions()).containsExactly("A", "B");
        assertThat(result.getData().getAnswer()).isEqualTo("A");
        assertThat(result.getData().getAnalysis()).isEqualTo("解析");
        assertThat(result.getData().getDifficulty()).isEqualTo(1);
    }

    @Test
    void getQuestionDetail_notFound_returnsNullData() {
        when(questionMapper.selectById(9L)).thenReturn(null);

        Result<QuestionDetailResponse> result = examQuestionApi.getQuestionDetail(9L);

        assertThat(result.getData()).isNull();
    }

    @Test
    void getQuestionsByIds_emptyInput_returnsEmptyWithoutQuery() {
        Result<List<QuestionBaseResponse>> result = examQuestionApi.getQuestionsByIds(List.of());

        assertThat(result.getData()).isEmpty();
        verifyNoInteractions(questionMapper);
    }

    @Test
    void updateStatistics_accepted_accumulatesCountsAndRate() {
        ExamQuestion question = new ExamQuestion();
        question.setId(1L);
        question.setSubmitCount(3);
        question.setAcceptedCount(1);
        when(questionMapper.selectById(1L)).thenReturn(question);

        Result<Void> result = examQuestionApi.updateStatistics(1L, true);

        assertThat(result).isNotNull();
        assertThat(question.getSubmitCount()).isEqualTo(4);
        assertThat(question.getAcceptedCount()).isEqualTo(2);
        assertThat(question.getAcceptanceRate()).isEqualByComparingTo(new BigDecimal("50.00"));
        verify(questionMapper).updateById(question);
    }

    @Test
    void updateStatistics_rejected_onlyIncreasesSubmit() {
        ExamQuestion question = new ExamQuestion();
        question.setId(2L);
        question.setSubmitCount(0);
        question.setAcceptedCount(0);
        when(questionMapper.selectById(2L)).thenReturn(question);

        examQuestionApi.updateStatistics(2L, false);

        assertThat(question.getSubmitCount()).isEqualTo(1);
        assertThat(question.getAcceptedCount()).isEqualTo(0);
        assertThat(question.getAcceptanceRate()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void getQuestionsForPrepare_nullChapter_returnsEmpty() {
        Result<List<QuestionBaseResponse>> result = examQuestionApi.getQuestionsForPrepare(null);

        assertThat(result.getData()).isEmpty();
        verifyNoInteractions(questionMapper);
    }
}
