package com.amlve.examapi.dto.response;

import lombok.Data;

/**
 * 管理端编程题测试用例视图。
 * <p>
 * 比学生端的 {@link QuestionDetailResponse.TestCaseItem} 多 weight / description 两个字段：
 * 教师修改编程题时要原样回填这些配置，缺了会在保存时被覆盖掉。
 */
@Data
public class QuestionTestCaseVO {

    /** 用例序号 */
    private Integer caseIndex;

    /** 输入 */
    private String input;

    /** 期望输出 */
    private String expectedOutput;

    /** 是否示例用例（示例下发给学生，其余隐藏） */
    private Boolean isSample;

    /** 用例类型：SAMPLE/NORMAL/BOUNDARY/SPECIAL/PERFORMANCE/ERROR */
    private String caseType;

    /** 分值权重 */
    private Integer weight;

    /** 时间上限（ms） */
    private Integer timeLimit;

    /** 内存上限（KB） */
    private Integer memoryLimit;

    /** 用例说明 */
    private String description;
}
