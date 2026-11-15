package com.amlve.study;

import com.amlve.common.constant.ErrorMessageConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.enums.QuestionTypeEnum;
import com.amlve.examapi.dto.response.QuestionDetailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 答案形态校验：只校验「答案长得对不对」，不判对错（判分交给 PracticeJudgeService）。
 */
@Slf4j
@Component
public class AnswerValidator {

    /** 主观题答案长度上限 */
    private static final int MAX_TEXT_ANSWER_LENGTH = 5000;
    /** 代码长度上限 */
    private static final int MAX_CODE_LENGTH = 20000;

    /** 选项前缀：A. / A、/ A) / A: / A 空格 */
    private static final Pattern OPTION_PREFIX =
            Pattern.compile("^\\s*([A-Za-z])[.、):：\\s]");

    /** 判断题合法取值 */
    private static final Set<String> JUDGE_TRUE =
            Set.of("对", "正确", "true", "t", "1", "yes", "y");
    private static final Set<String> JUDGE_FALSE =
            Set.of("错", "错误", "false", "f", "0", "no", "n");

    public void validate(QuestionDetailResponse question, String answer) {
        QuestionTypeEnum type = QuestionTypeEnum.fromCode(question.getType());
        if (type == null) {
            log.warn("QUESTION_TYPE_INVALID 定位: {}", question.getType());
            throw new BusinessException(ErrorMessageConstant.QUESTION_TYPE_INVALID);
        }
        if (answer == null || answer.isBlank()) {
            throw new BusinessException(ErrorMessageConstant.ANSWER_MISSING);
        }

        switch (type) {
            case CHOICE -> validateChoice(question, answer);
            case JUDGE -> validateJudge(answer);
            case PROGRAMMING -> validateProgramming(answer);
            case FILL_IN, PROGRAM_READ, SHORT_ANSWER -> validateTextLength(answer);
        }
    }

    // ===== 选择题 =====

    private void validateChoice(QuestionDetailResponse question, String answer) {
        List<String> options = question.getOptions();
        if (options == null || options.isEmpty()) {
            // 题目数据本身有问题，直接拒绝，避免把脏答案判成「错误」写进练习记录
            log.warn("QUESTION_OPTIONS_MISSING 定位: {}", question.getId());
            throw new BusinessException(ErrorMessageConstant.QUESTION_OPTIONS_MISSING);
        }
        Set<Character> legalKeys = legalOptionKeys(options);
        Set<Character> picked = parseOptionKeys(answer);
        if (picked.isEmpty()) {
            log.warn("CHOICE_ANSWER_INVALID 定位: {}", answer);
            throw new BusinessException(ErrorMessageConstant.CHOICE_ANSWER_INVALID);
        }
        for (Character key : picked) {
            if (!legalKeys.contains(key)) {
                throw new BusinessException(
                        "选择题答案超出选项范围: " + key + "，可选: " + legalKeys);
            }
        }
    }

    /**
     * 推导合法选项键：优先取选项文本自带前缀（A. B.），
     * 没有前缀时按位置兜底为 A/B/C/D，兼容两种录入格式。
     */
    private Set<Character> legalOptionKeys(List<String> options) {
        Set<Character> keys = new LinkedHashSet<>();
        for (String option : options) {
            Matcher m = OPTION_PREFIX.matcher(option == null ? "" : option);
            if (m.find()) {
                keys.add(Character.toUpperCase(m.group(1).charAt(0)));
            }
        }
        if (keys.size() == options.size()) {
            return keys;
        }
        keys.clear();
        for (int i = 0; i < options.size() && i < 26; i++) {
            keys.add((char) ('A' + i));
        }
        return keys;
    }

    /** 解析学生答案中的选项键，支持 "A"、"AB"、"A,B"、"A. 选项内容" */
    private Set<Character> parseOptionKeys(String answer) {
        Set<Character> picked = new LinkedHashSet<>();
        Matcher prefix = OPTION_PREFIX.matcher(answer.trim());
        if (prefix.find()) {
            picked.add(Character.toUpperCase(prefix.group(1).charAt(0)));
            return picked;
        }
        for (char c : answer.replaceAll("[\\s,，;；/|]", "").toCharArray()) {
            if (Character.isLetter(c)) {
                picked.add(Character.toUpperCase(c));
            }
        }
        return picked;
    }

    // ===== 判断题 =====

    private void validateJudge(String answer) {
        String value = answer.trim().toLowerCase(Locale.ROOT);
        if (!JUDGE_TRUE.contains(value) && !JUDGE_FALSE.contains(value)) {
            log.warn("JUDGE_ANSWER_INVALID 定位: {}", answer);
            throw new BusinessException(ErrorMessageConstant.JUDGE_ANSWER_INVALID);
        }
    }

    // ===== 编程题 =====

    private void validateProgramming(String answer) {
        if (answer.isBlank()) {
            throw new BusinessException(ErrorMessageConstant.CODE_MISSING);
        }
        if (answer.length() > MAX_CODE_LENGTH) {
            throw new BusinessException("代码长度超出上限（" + MAX_CODE_LENGTH + " 字符）");
        }
        // 注意：是否含 public class / main 方法由 CodeRunServiceImpl 的预检负责，
        // 这里不重复校验，避免把「可提交但编译不过」的代码拦在门外。
    }

    // ===== 文本题 =====

    private void validateTextLength(String answer) {
        if (answer.length() > MAX_TEXT_ANSWER_LENGTH) {
            throw new BusinessException("答案长度超出上限（" + MAX_TEXT_ANSWER_LENGTH + " 字符）");
        }
    }
}
