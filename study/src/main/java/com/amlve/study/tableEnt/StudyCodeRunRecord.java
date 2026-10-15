package com.amlve.study.tableEnt;

import com.amlve.agentcodeapi.dto.response.CodeQualityDTO;
import com.amlve.agentcodeapi.dto.response.CommonMistakeDTO;
import com.amlve.agentcodeapi.dto.response.ComplexityDTO;
import com.amlve.agentcodeapi.dto.response.ImprovementDTO;
import com.amlve.agentcodeapi.dto.response.RelatedKnowledgeDTO;
import com.amlve.agentcodeapi.dto.response.SimilarQuestionDTO;
import com.amlve.agentcodeapi.dto.response.StepDTO;
import com.amlve.common.entity.BaseEntity;
import com.amlve.common.enums.JudgeResultEnum;
import com.amlve.common.enums.RunStatusEnum;
import com.amlve.study.dto.response.TestResultVO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 学生代码运行记录（表：study_code_run_record）
 *
 * <p>每次调用「编译运行 / 判题」接口都会写入一条记录，用于学情分析与 AI 调试回溯。
 *
 * <p>状态口径（任务书 5.2 定稿）：
 * <ul>
 *   <li>{@code judgeStatus}（AC/WA/TLE/MLE/RE/CE/OLE/SV/SE）为<b>权威运行状态</b></li>
 *   <li>旧列 {@code status}（1/2/3/4/5）保留为兼容期字段，已标 deprecated</li>
 * </ul>
 *
 * <p>JSON 列（test_result / error_line_list / code_quality / complexity / steps /
 * common_mistakes / improvements / related_knowledge / similar_questions）
 * 统一使用 {@link JacksonTypeHandler}，配合 {@code autoResultMap = true}，
 * 泛型由 MyBatis-Plus 3.5.6+ 基于 Field 解析，查询可正确回填。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "study_code_run_record", autoResultMap = true)
public class StudyCodeRunRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 提交用户ID（来自 Token）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 关联章节ID（自由练习时必填）
     */
    @TableField("chapter_id")
    private Long chapterId;

    /**
     * 关联题目ID（自由练习时为 null）
     */
    @TableField("question_id")
    private Long questionId;

    /**
     * 学生提交的源代码
     */
    @TableField("code_content")
    private String codeContent;

    /**
     * 运行状态（1=成功，2=编译错误，3=运行时错误，4=未通过安全检查，5=系统内部错误）
     *
     * @deprecated 兼容期字段，权威状态请认 {@link #judgeStatus}
     */
    @Deprecated
    private Integer status;

    /**
     * 权威运行状态：AC/WA/TLE/MLE/RE/CE/OLE/SV/SE
     */
    @TableField("judge_status")
    private RunStatusEnum judgeStatus;

    /**
     * 判题结果：全部通过 / 部分通过 / 未判题
     */
    @TableField("judge_result")
    private JudgeResultEnum judgeResult;

    /**
     * 测试用例执行结果（{cases:[…], passedCount, totalCount}）
     */
    @TableField(value = "test_result", typeHandler = JacksonTypeHandler.class)
    private TestResultVO testResult;

    /**
     * 通过用例数
     */
    @TableField("passed_count")
    private Integer passedCount;

    /**
     * 总用例数
     */
    @TableField("total_count")
    private Integer totalCount;

    /**
     * 运行耗时（毫秒）
     */
    @TableField("run_duration")
    private Integer runDuration;

    /**
     * 内存占用（KB）。当前运行器未采集子进程 RSS，固定为 0，列先建好
     */
    @TableField("memory_used")
    private Integer memoryUsed;

    /**
     * 错误行号（编译器报的单个）
     */
    @TableField("error_line")
    private Integer errorLine;

    /**
     * 错误行号列表（AI 分析，可能多个）
     */
    @TableField(value = "error_line_list", typeHandler = JacksonTypeHandler.class)
    private List<String> errorLineList;

    /**
     * 程序运行结果（标准输出）
     */
    @TableField("run_result")
    private String runResult;

    /**
     * 错误信息（编译/运行/安全拦截/系统错误）
     */
    @TableField("error_msg")
    private String errorMsg;

    // ========== AI 调试字段（完整丰富版） ==========

    /**
     * 错误原因（人话解释）
     */
    @TableField("error_reason")
    private String errorReason;

    /**
     * 优化建议
     */
    @TableField("optimize_suggest")
    private String optimizeSuggest;

    /**
     * 标准解题思路
     */
    @TableField("standard_idea")
    private String standardIdea;

    /**
     * 关联知识点（JSON 或逗号分隔字符串）
     */
    @TableField("knowledge_points")
    private String knowledgePoints;

    /**
     * 代码质量评分
     */
    @TableField(value = "code_quality", typeHandler = JacksonTypeHandler.class)
    private CodeQualityDTO codeQuality;

    /**
     * 复杂度分析
     */
    @TableField(value = "complexity", typeHandler = JacksonTypeHandler.class)
    private ComplexityDTO complexity;

    /**
     * 分步引导（学生自己思考的路径，不给答案）
     */
    @TableField(value = "steps", typeHandler = JacksonTypeHandler.class)
    private List<StepDTO> steps;

    /**
     * 常见错误对比
     */
    @TableField(value = "common_mistakes", typeHandler = JacksonTypeHandler.class)
    private List<CommonMistakeDTO> commonMistakes;

    /**
     * 改进建议列表
     */
    @TableField(value = "improvements", typeHandler = JacksonTypeHandler.class)
    private List<ImprovementDTO> improvements;

    /**
     * 关联教材章节 + 页码
     */
    @TableField(value = "related_knowledge", typeHandler = JacksonTypeHandler.class)
    private List<RelatedKnowledgeDTO> relatedKnowledge;

    /**
     * 相似题推荐
     */
    @TableField(value = "similar_questions", typeHandler = JacksonTypeHandler.class)
    private List<SimilarQuestionDTO> similarQuestions;

    /**
     * 鼓励性话语
     */
    @TableField("encouragement")
    private String encouragement;

    /**
     * 学习建议（后续学习方向）
     */
    @TableField("learning_suggestion")
    private String learningSuggestion;
}
