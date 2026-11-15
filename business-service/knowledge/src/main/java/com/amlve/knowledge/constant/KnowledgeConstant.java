package com.amlve.knowledge.constant;

import java.util.Set;

/**
 * knowledge 模块常量
 */
public final class KnowledgeConstant {

    // ===== Dify 检索结果 metadata 中的键名（与 Dify 知识库元数据字段一致） =====

    /** 元数据键：章节ID */
    public static final String METADATA_CHAPTER_ID = "chapter_id";

    /** 元数据键：章节名称 */
    public static final String METADATA_CHAPTER_NAME = "chapter_name";

    /** 元数据键：页码 */
    public static final String METADATA_PAGE = "page";

    // ===== 知识库文件上传（Dify 支持的文档类型，与官方白名单一致） =====

    /**
     * Dify 知识库支持的上传文件扩展名（小写）；zip 等压缩包与无扩展名文件会被 Dify 拒绝，
     * 在入口处提前拦截并返回明确错误，而不是透传 Dify 的 400
     */
    public static final Set<String> UPLOAD_FILE_ALLOWED_EXTENSIONS = Set.of(
            "txt", "md", "markdown", "pdf", "html", "htm", "docx", "xlsx", "csv", "pptx", "xml", "epub");

    private KnowledgeConstant() {
    }
}
