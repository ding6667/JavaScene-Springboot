package com.amlve.teachapi.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class HomeworkPublishRequest {

    @NotBlank(message = "作业标题不能为空")
    private String title;

    @NotBlank(message = "作业内容不能为空")
    private String content;

    @NotNull(message = "章节ID不能为空")
    private Long chapterId;

    @NotNull(message = "截止时间不能为空")
    private LocalDateTime deadline;

    private Long classId;  // NULL表示全年级

    private List<Long> questionIds;  // 选择的题目ID列表
}