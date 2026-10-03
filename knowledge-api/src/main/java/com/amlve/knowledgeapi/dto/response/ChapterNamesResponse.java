package com.amlve.knowledgeapi.dto.response;

import java.util.List;

public class ChapterNamesResponse {
    private List<ChapterNameResponse> chapters;   /** 命中的章节 */
    private List<Long> notFoundIds;               /** 未命中的 chapterId（不整体报错） */
}
