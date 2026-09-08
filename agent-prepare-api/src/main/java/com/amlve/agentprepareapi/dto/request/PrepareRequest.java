package com.amlve.agentprepareapi.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class PrepareRequest {

    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    /**
     * 是否生成 PPT（1=开启，0=关闭）
     */
    private String pptx = "1";

    /**
     * 是否生成教案（1=开启，0=关闭）
     */
    private String lessonPlan = "1";

    /**
     * 是否生成课后习题（1=开启，0=关闭）
     */
    private String homework = "1";

    /**
     * 是否生成上机实训方案（1=开启，0=关闭）
     */
    private String lab = "1";

    /**
     * 是否提炼重难点（1=开启，0=关闭）
     */
    private String keyPoints = "1";

    /**
     * 用户额外消息（可选）
     */
    private String userMessage;
}