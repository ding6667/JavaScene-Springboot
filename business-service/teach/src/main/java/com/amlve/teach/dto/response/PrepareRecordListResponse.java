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

        /** 是否已生成PPT正文（pptx_md 非空）：有正文即代表能下载，文件没转出来时下载接口会现场补生成 */
        private Boolean hasPptMd;

        /**
         * PPT 三态：0=无课件内容，1=有正文·文件未产出（转换中/转换失败/文件已清理），2=文件就绪
         * <p>
         * 与 hasPpt/hasPptMd 冗余但更直观，前端做角标/按钮文案时可直接用；
         * 关系恒为：hasPpt=(pptStatus==2)，hasPptMd=(pptStatus>=1)。
         */
        private Integer pptStatus;

        /**
         * 是否已生成PPT文件（文件真实存在，不是「URL 非空」）。
         * <p>
         * 异步转换尚未完成、或文件已被清理时为 false；此时前端应仍按 hasPptMd 显示「有 PPT」并允许点击下载。
         */
        private Boolean hasPpt;
    }
}
