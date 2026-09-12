package com.amlve.study.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudyProgressSaveRequest {
    /**
     * 章节ID
     */
    @NotNull(message = "章节id不能为空")
    private Long chapterId;
    /**
     * 本章节的学习进度(0-100)，达到 100 时服务端自动标记章节完成
     */
    @NotNull(message = "学习进度不能为空")
    @Min(value = 0, message = "学习进度不能小于0")
    @Max(value = 100, message = "学习进度不能大于100")
    private Integer progressRate;
    /**
     * 累计学习时长
     */
    private Integer studyDuration;

    private Integer isCompleted;
}
