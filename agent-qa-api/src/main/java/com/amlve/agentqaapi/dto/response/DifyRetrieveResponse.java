package com.amlve.agentqaapi.dto.response;



import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Dify 知识库检索响应
 */
@Data
public class DifyRetrieveResponse {
    private String query;
    private List<Record> records;

    @Data
    public static class Record {
        private Segment segment;
        private Double score;
    }

    @Data
    public static class Segment {
        private String id;
        private String content;
        private Document document;
        private Map<String, Object> metadata;  // 这里会拿到章节ID、页码等，常量建议封装在COnstant
    }

    @Data
    public static class Document {
        private String id;
        private String name;
    }
}