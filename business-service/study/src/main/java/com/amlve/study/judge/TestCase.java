package com.amlve.study.judge;

/**
 * 判题用例（由 QuestionDetailResponse.TestCaseItem 转换，
 * 目的是让 CodeRunService 不依赖 exam-api 的 DTO）
 */
public record TestCase(
        int caseIndex,
        String input,
        String expectedOutput,
        /** 时间限制，单位：毫秒；为 null 时用默认值，见 CodeRunServiceImpl#resolveCaseTimeout */
        Integer timeLimitMs,
        /** 是否为示例用例 */
        Boolean isSample,
        /** 用例类型：SAMPLE/NORMAL/BOUNDARY/SPECIAL/PERFORMANCE/ERROR */
        String caseType
) {}
