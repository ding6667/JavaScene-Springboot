package com.amlve.knowledge.converter;

import com.amlve.knowledge.dto.response.ChapterResponse;
import com.amlve.knowledge.tableEnt.KnowledgeChapter;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface KnowledgeConverter {

    ChapterResponse toResponse(KnowledgeChapter chapter);

    List<ChapterResponse> toResponseList(List<KnowledgeChapter> chapters);

    // ===== 跨模块 DTO 映射（knowledge-api） =====

    com.amlve.knowledgeapi.dto.response.ChapterDTO toDTO(KnowledgeChapter chapter);

    List<com.amlve.knowledgeapi.dto.response.ChapterDTO> toChapterDTOList(List<KnowledgeChapter> chapters);

    com.amlve.knowledgeapi.dto.response.KnowledgePointDTO toDTO(com.amlve.knowledge.tableEnt.KnowledgePoint point);

    List<com.amlve.knowledgeapi.dto.response.KnowledgePointDTO> toKnowledgePointDTOList(
            List<com.amlve.knowledge.tableEnt.KnowledgePoint> points);

    /**
     * 内部检索结果 → knowledge-api 对外响应（跨模块 DTO 手动映射）
     */
    default List<com.amlve.knowledgeapi.dto.response.KnowledgeResponse.KnowledgeItem> toKnowledgeItems(
            List<com.amlve.knowledge.dto.response.KnowledgeResponse.KnowledgeItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream().map(this::toKnowledgeItem).collect(java.util.stream.Collectors.toList());
    }

    default com.amlve.knowledgeapi.dto.response.KnowledgeResponse.KnowledgeItem toKnowledgeItem(
            com.amlve.knowledge.dto.response.KnowledgeResponse.KnowledgeItem item) {
        if (item == null) {
            return null;
        }
        com.amlve.knowledgeapi.dto.response.KnowledgeResponse.KnowledgeItem result =
                new com.amlve.knowledgeapi.dto.response.KnowledgeResponse.KnowledgeItem();
        result.setSegmentId(item.getSegmentId());
        result.setContent(item.getContent());
        result.setScore(item.getScore());
        result.setDocumentId(item.getDocumentId());
        result.setDocumentName(item.getDocumentName());
        result.setChapterId(item.getChapterId());
        result.setPage(item.getPage());
        return result;
    }
}
