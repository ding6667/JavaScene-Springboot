package com.amlve.study.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 测试用例执行结果（任务书 2.4 / 3.6 公共结构）
 *
 * <p>{@link CaseVO} 同时用于运行结果响应（原 TestCaseResult）与运行记录详情
 * （原 CaseVO）两处，只保留这一份定义，不得重复声明。
 */
@Data
public class TestResultVO {

    /**
     * 用例明细
     */
    private List<CaseVO> cases;

    /**
     * 通过用例数
     */
    private Integer passedCount;

    /**
     * 总用例数
     */
    private Integer totalCount;

    @Data
    public static class CaseVO {

        /**
         * 用例序号
         */
        private Integer caseIndex;

        /**
         * 标准输入
         */
        private String input;

        /**
         * 期望输出
         */
        private String expectedOutput;

        /**
         * 实际输出
         */
        private String actualOutput;

        /**
         * 是否通过
         */
        private Boolean passed;

        /**
         * 是否为示例用例
         */
        private Boolean isSample;

        /**
         * 用例类型：SAMPLE/NORMAL/BOUNDARY/SPECIAL/PERFORMANCE/ERROR
         */
        private String caseType;

        /**
         * 用例描述
         */
        private String description;

        /**
         * 单用例耗时（毫秒）
         */
        private Integer duration;

        /**
         * 单用例内存（KB）
         */
        private Integer memory;

        /**
         * 失败原因（通过时为 null）
         */
        private String error;
    }
}
