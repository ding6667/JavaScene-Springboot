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

    /**
     * PPT 模板ID（teach 侧解析模板后透传，智能体侧不直接使用）
     */
    private String templateId;

    /**
     * PPT 生成配置（结构骨架 + 页数等约束，由 teach 侧填充模板骨架）
     */
    private PptConfig pptConfig;

    /** PPT 视觉风格令牌（JSON 字符串，来自模板 designTokens；空串表示用默认风格） */
    private String styleTokens;
    /**
     * 教案生成配置（课型/时长/详细度等）
     */
    private LessonPlanConfig lessonPlanConfig;

    /**
     * 课后习题配置（题量/难度分布/题库）
     */
    private HomeworkConfig homeworkConfig;

    /**
     * PPT 生成配置：字段全 String（Dify 端均为字符串输入，未传由工作流兜底默认值）
     */
    @Data
    public static class PptConfig {

        /** 内容结构 MD 骨架（teach 侧从模板 structure_md 填充，空串降级） */
        private String structure;

        /** 页数范围，如 "10-14" */
        private String pageRange;

        /** 是否包含目录页：1=是 */
        private String includeOutline;

        /** 是否包含总结页：1=是 */
        private String includeSummary;

        /** 每页代码块上限 */
        private String codeBlockMax;
    }

    /**
     * 教案生成配置
     */
    @Data
    public static class LessonPlanConfig {

        /** 课型：new=新授课，review=复习课等 */
        private String courseType;

        /** 课时分钟数，如 "45" */
        private String duration;

        /** 详细度：brief=简案，medium=标准，detailed=详案 */
        private String detailLevel;

        /** 是否包含板书设计：1=是 */
        private String includeBoardDesign;

        /** 是否包含课堂练习环节：1=是 */
        private String includeExercise;
    }

    /**
     * 课后习题配置
     */
    @Data
    public static class HomeworkConfig {

        /** 题目数量，如 "5" */
        private String questionCount;

        /** 难度分布（易,中,难），如 "2,2,1" */
        private String difficultyDistribution;

        /** 题库列表 JSON 串（teach 侧从 exam-api 拉取，未就绪传空串降级） */
        private String questionBank;
    }
}