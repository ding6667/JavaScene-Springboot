package com.amlve.agentcodeapi.dto.response;

import lombok.Data;

/**
 * 分步引导（学生自己思考的路径，不给答案）
 */
@Data
public class StepDTO {

    /**
     * 步骤序号
     */
    private Integer stepNo;

    /**
     * 步骤标题
     */
    private String title;

    /**
     * 步骤内容（引导式，不给答案）
     */
    private String content;

    /**
     * 关联代码位置
     */
    private String codeLocation;
}
