package com.amlve.study.judge;

import com.amlve.common.constant.ErrorMessageConstant;
import com.amlve.common.enums.QuestionTypeEnum;
import com.amlve.common.exception.BusinessException;
import com.amlve.examapi.dto.response.QuestionDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class JudgeService {
    /**
     * 标准答案侧多空分隔符
     */
    private static final Pattern STD_BLANK_SPLIT = Pattern.compile("\\s*\\|\\s*");
    /**
     * 学生答案侧多空分隔符：兼容 | ; ； 换行
     */
    private static final Pattern STU_BLANK_SPLIT = Pattern.compile("\\s*[|;；]\\s*|\\s*\\n+\\s*");
    /**
     * 单空内多个等价答案分隔符：操作系统/OS
     */
    private static final Pattern EQUIV_SPLIT = Pattern.compile("\\s*/\\s*");

    public JudgeResult judge(QuestionDetailResponse question, String studentAnswer) {
        QuestionTypeEnum type = QuestionTypeEnum.fromCode(question.getType());
        if (type == null || !type.isObjective()) {
            throw new BusinessException(ErrorMessageConstant.QUESTION_TYPE_UNSUPPORTED
                    + question.getId() + ", type = "
                    + question.getType());
        }
        String questionAnswer = question.getAnswer();
        if (questionAnswer == null || questionAnswer.isBlank()) {
            //该题目的标准答案缺失
            log.warn("STANDARD_ANSWER_MISSING 定位: {}", question.getId());
            throw new BusinessException(ErrorMessageConstant.STANDARD_ANSWER_MISSING);
        }
        return switch (type) {
            case CHOICE -> judgeChoice(questionAnswer, studentAnswer, question);
            case FILL_IN -> judgeFillIn(questionAnswer, studentAnswer, question);
            case JUDGE -> judgeJudge(questionAnswer, studentAnswer, question);
            case PROGRAM_READ -> judgeProgramRead(questionAnswer, studentAnswer, question);
            default -> throw new BusinessException(ErrorMessageConstant.QUESTION_TYPE_UNSUPPORTED
                    + question.getId() + ", type = "
                    + question.getType());
        };
    }

    private JudgeResult judgeProgramRead(String questionAnswer, String studentAnswer, QuestionDetailResponse question) {
        String expected = AnswerNormalizer.multiline(questionAnswer);
        String actual = AnswerNormalizer.multiline(studentAnswer);
        boolean correct = expected.equals(actual)
                // 兜底：学生把多行输出写成一行 / 用空格分隔
                || AnswerNormalizer.compact(expected).equals(AnswerNormalizer.compact(actual));
        return JudgeResult.of(correct, question.getScore(), actual, expected);

    }

    private JudgeResult judgeJudge(String questionAnswer, String studentAnswer, QuestionDetailResponse question) {
        Boolean expected = AnswerNormalizer.toBoolean(questionAnswer);
        if (expected == null) {
            log.warn("STANDARD_ANSWER_INVALID 定位: {}", question.getId());
            throw new BusinessException(ErrorMessageConstant.STANDARD_ANSWER_INVALID);
        }
        Boolean actual = AnswerNormalizer.toBoolean(studentAnswer);
        return JudgeResult.of(expected.equals(actual), question.getScore(),
                String.valueOf(actual), String.valueOf(expected));
    }

    private JudgeResult judgeFillIn(String questionAnswer, String studentAnswer, QuestionDetailResponse question) {
        List<String> expectedBlanks = splitBlanks(STD_BLANK_SPLIT, questionAnswer);
        List<String> givenBlanks = splitBlanks(STU_BLANK_SPLIT, studentAnswer);

        List<JudgeResult.BlankResult> details = new ArrayList<>();
        int hit = 0;
        for (int i = 0; i < expectedBlanks.size(); i++) {
            List<String> equivalents = splitBlanks(EQUIV_SPLIT, expectedBlanks.get(i));
            String given = i < givenBlanks.size() ? givenBlanks.get(i) : "";
            boolean ok = !given.isBlank() && equivalents.stream()
                    .anyMatch(e -> AnswerNormalizer.text(e).equals(AnswerNormalizer.text(given)));
            if (ok) hit++;
            details.add(new JudgeResult.BlankResult(i + 1, ok, String.join("/", equivalents), given));
        }
        boolean correct = hit == expectedBlanks.size() && givenBlanks.size() == expectedBlanks.size();
        int score = correct ? question.getScore()
                : (!expectedBlanks.isEmpty()
                   ? Math.round(question.getScore() * (float) hit / expectedBlanks.size()) : 0);

        return new JudgeResult(correct, score, question.getScore(), List.copyOf(details),
                String.join("|", givenBlanks), String.join("|", expectedBlanks));
    }

    private JudgeResult judgeChoice(String questionAnswer, String studentAnswer, QuestionDetailResponse question) {
        //标准答案集合
        Set<Character> expected = OptionKeys.resolve(questionAnswer, question.getOptions());
        //学生答案集合
        Set<Character> actual = OptionKeys.resolve(studentAnswer, question.getOptions());
        return JudgeResult.of(expected.equals(actual), question.getScore(),
                OptionKeys.keysToString(actual), OptionKeys.keysToString(expected));
    }

    /**
     * 按分隔符切分，只丢首尾空项（中间的空格位要保留，代表该空没填）
     */
    private List<String> splitBlanks(Pattern pattern, String raw) {
        List<String> parts = Arrays.stream(pattern.split(raw == null ? "" : raw, -1))
                .map(String::trim)
                .collect(Collectors.toCollection(ArrayList::new));
        while (!parts.isEmpty() && parts.get(0).isEmpty()) parts.remove(0);
        while (!parts.isEmpty() && parts.get(parts.size() - 1).isEmpty()) parts.remove(parts.size() - 1);
        return parts;
    }
}
