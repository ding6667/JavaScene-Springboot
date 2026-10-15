package com.amlve.studyapi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 刷题个人状态跨模块 DTO
 * 供 exam 模块拼装刷题列表与详情使用
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PracticeStatusResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 题目ID
     */
    private Long questionId;

    /**
     * 是否已做
     */
    private Boolean done;

    /**
     * 是否做对
     */
    private Boolean correct;

    /**
     * 尝试次数
     */
    private Integer attemptCount;

    /**
     * 学生上次提交的答案
     */
    private String lastAnswer;

    /**
     * 是否收藏
     * 备注：当前 study_practice_record / study_question_status 均无收藏列，
     * 实现侧可先恒返回 false，后续补收藏表或收藏列后再接真实数据。
     */
    private Boolean favorited = Boolean.FALSE;
}