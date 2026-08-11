package com.amlve.dialogueapi.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentJson {
    /**
     *         "title": { "type": "string", "description": "标题" },
     *         "scoring_table": { "type": "string", "description": "评分表格" },
     *         "mistakes": { "type": "string", "description": "错误" },
     *         "advantages": { "type": "string", "description": "优点" }
     *         "summary": {"type": "string","description": "总结"
     */
    private String title;
    private String scoringTable;
    private String mistakes;
    private String advantages;
    private String summary;

}
