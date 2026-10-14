package com.amlve.knowledgeapi.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 章节名称批量查询请求
 */
@Data
public class ChapterNamesRequest {

    /**
     * 章节ID列表：服务端会先去重再查询，最多 100 个（超出直接参数错误，避免 IN 过大）
     */
    @NotEmpty(message = "章节ID列表不能为空")
    @Size(max = 100, message = "单次最多查询 100 个章节")
    private List<Long> chapterIds;
}
