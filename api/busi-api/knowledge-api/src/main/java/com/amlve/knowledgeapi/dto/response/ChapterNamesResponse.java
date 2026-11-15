package com.amlve.knowledgeapi.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 章节名称批量查询响应
 */
@Data
public class ChapterNamesResponse {

    /**
     * 命中的章节（顺序与入参去重后的顺序无必然关系，调用方按 chapterId 取用）
     */
    private List<ChapterNameDTO> chapters;

    /**
     * 未命中的章节ID（如已逻辑删除或不存在的章节）。
     * 批量查询不因个别未命中整体报错，由调用方决定如何提示。
     */
    private List<Long> notFoundIds;
}
