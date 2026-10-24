package com.amlve.teach.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

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

    /**
     * PPT 模板ID（可选）：为空时使用系统默认模板；
     * 模板 themeCode 用于注入 PPT 视觉主题，structureMd 作为 PPT 生成骨架
     */
    private Long templateId;

    /**
     * PPT 风格令牌（可选）：传了则以该值为准，否则回退到模板 themeCode 对应的预置令牌。
     * 当前仅用于后端 PPT Markdown 样式覆写；agent-prepare-api 契约无此字段，暂不透传 Dify。
     */
    private Map<String, Object> styleTokens;

    /**
     * PPT 生成配置（可选，未传字段由工作流兜底默认值）
     */
    private PptConfig pptConfig;

    /**
     * 教案生成配置（可选）
     */
    private LessonPlanConfig lessonPlanConfig;

    /**
     * 课后习题配置（可选；questionBank 由后端从题库拉取填充，前端无需传）
     */
    private HomeworkConfig homeworkConfig;

    /**
     * PPT 生成配置：字段全 String，与智能体 api 契约一致
     */
    @lombok.Data
    public static class PptConfig {

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
    @lombok.Data
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
    @lombok.Data
    public static class HomeworkConfig {

        /** 题目数量，如 "5" */
        private String questionCount;

        /** 难度分布（易,中,难），如 "2,2,1" */
        private String difficultyDistribution;
    }
}
