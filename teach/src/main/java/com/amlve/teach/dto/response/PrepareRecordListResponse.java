package com.amlve.teach.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 备课记录分页列表响应（教师端）
 */
@Data
public class PrepareRecordListResponse {

    /** 当前页数据 */
    private List<Item> records;

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private long pageNum;

    /** 每页大小 */
    private long pageSize;

    /**
     * 备课记录列表项（不含材料全文，仅展示是否有值）
     */
    @Data
    public static class Item {

        /** 备课记录ID */
        private Long id;

        /** 关联教材章节ID */
        private Long chapterId;

        /** 备课时间 */
        private LocalDateTime createTime;

        /** 是否有教案 */
        private Boolean hasLessonPlan;

        /** 是否有重难点清单 */
        private Boolean hasKeyPoints;

        /** 是否有课后习题 */
        private Boolean hasHomework;

        /** 是否有上机实训方案 */
        private Boolean hasLab;

        /** 是否已生成PPT文件 */
        private Boolean hasPpt;
    }
}
