package com.amlve.knowledgeapi.dto.request;

import java.util.List;

public class ChapterNamesRequest {
    private List<Long> chapterIds;   // 必填，去重后最多 100 个
}
