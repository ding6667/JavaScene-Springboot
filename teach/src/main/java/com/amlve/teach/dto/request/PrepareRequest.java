package com.amlve.teach.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 教师生成教案的请求参数（前端提交用）
 */
@Data
public class PrepareRequest {

    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    /**
     * 是否生成 PPT（1=开启，0=关闭），默认开启，透传给备课智能体
     */
    private String pptx = "1";

    /**
     * 是否生成教案（1=开启，0=关闭），默认开启，透传给备课智能体
     */
    private String lessonPlan = "1";

    /**
     * 是否生成课后习题（1=开启，0=关闭），默认开启，透传给备课智能体
     */
    private String homework = "1";

    /**
     * 是否生成上机实训方案（1=开启，0=关闭），默认开启，透传给备课智能体
     */
    private String lab = "1";

    /**
     * 是否提炼重难点（1=开启，0=关闭），默认开启，透传给备课智能体
     */
    private String keyPoints = "1";

    /**
     * 用户额外指令（可选），透传给备课智能体
     */
    private String userMessage;
}
