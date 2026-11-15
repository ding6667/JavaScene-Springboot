package com.amlve.study.judge;

import java.util.List;

/** 客观题本地判分结果 */
public record JudgeResult(
        boolean correct,
        int score,
        int fullScore,
        /** 填空题逐空明细；非填空为 List.of() */
        List<BlankResult> blanks,
        String normalizedStudent,
        String normalizedStandard
) {
    public record BlankResult(int index, boolean correct, String expected, String given) {}

    public static JudgeResult of(boolean correct, int fullScore, String student, String standard) {
        return new JudgeResult(correct, correct ? fullScore : 0, fullScore, List.of(), student, standard);
    }
}
