package com.amlve.study.dto.response;

import lombok.Data;

/**
 * 刷题草稿保存响应（任务书 3.4）
 */
@Data
public class PracticeDraftResponse {

    /**
     * 练习记录ID
     */
    private Long practiceId;

    /**
     * 保存状态：1=成功
     */
    private Integer saveStatus;
}
