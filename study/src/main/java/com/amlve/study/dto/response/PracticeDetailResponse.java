package com.amlve.study.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 刷题作答详情响应（任务书 3.3）
 *
 * <p>无记录时返回空结构（practiceId=null、status=0、runDetail=null），
 * 不返回 404，前端按「未作答」渲染。
 */
@Data
public class PracticeDetailResponse {

    /**
     * 练习记录ID
     */
    private Long practiceId;

    /**
     * 题目ID
     */
    private Long questionId;

    /**
     * 学生答案
     */
    private String answerContent;

    /**
     * 状态：0=草稿，1=已提交
     */
    private Integer status;

    /**
     * 是否正确
     */
    private Integer isCorrect;

    /**
     * 耗时（秒）
     */
    private Integer duration;

    /**
     * 尝试次数
     */
    private Integer attemptCount;

    /**
     * 提交时间（草稿也会更新）
     */
    private LocalDateTime submitTime;

    /**
     * 关联的代码运行记录ID（指向最新一次运行）
     */
    private Long runRecordId;

    /**
     * 代码运行结果摘要（只取最新一次，历史运行走记录列表查）
     */
    private CodeRunDetail runDetail;

    @Data
    public static class CodeRunDetail {
        private String output;
        private String errorMsg;
        private Integer errorLine;
        private String errorReason;
        private String optimizeSuggest;
        private String standardIdea;
        private List<String> knowledgePoints;
    }
}
