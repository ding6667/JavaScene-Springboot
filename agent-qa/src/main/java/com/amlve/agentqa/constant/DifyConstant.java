package com.amlve.agentqa.constant;

/**
 * Dify 知识库 API 常量
 * <p>
 * 端点定义参考：https://docs.dify.ai/zh/api-reference/guides/knowledge
 */
public final class DifyConstant {

    /** 从文本创建文档 */
    public static final String CREATE_DOCUMENT_BY_TEXT = "/datasets/{datasetId}/document/create-by-text";

    /** 从文件创建文档 */
    public static final String CREATE_DOCUMENT_BY_FILE = "/datasets/{datasetId}/document/create-by-file";

    /** 批量更新文档元数据 */
    public static final String UPDATE_DOCUMENT_METADATA = "/datasets/{datasetId}/documents/metadata";

    /** 查询文档索引状态（按创建时返回的 batch 查询，含 error 信息） */
    public static final String DOCUMENT_INDEXING_STATUS =
            "/datasets/{datasetId}/documents/{batch}/indexing-status";

    /** 从知识库检索分段 */
    public static final String RETRIEVE = "/datasets/{datasetId}/retrieve";

    /** 查询/创建知识库元数据字段 */
    public static final String DATASET_METADATA = "/datasets/{datasetId}/metadata";

    /** 元数据字段类型：数值 */
    public static final String METADATA_TYPE_NUMBER = "number";

    /** 元数据字段类型：文本 */
    public static final String METADATA_TYPE_STRING = "string";

    /** 索引方式：高质量（嵌入模型，精确检索） */
    public static final String INDEXING_TECHNIQUE_HIGH_QUALITY = "high_quality";

    /** 分块处理规则：自动 */
    public static final String PROCESS_MODE_AUTOMATIC = "automatic";

    /** 检索方式：混合检索（语义 + 全文） */
    public static final String SEARCH_METHOD_HYBRID = "hybrid_search";

    /** 检索默认返回条数 */
    public static final int DEFAULT_TOP_K = 5;

    /** 检索默认相似度阈值 */
    public static final double DEFAULT_SCORE_THRESHOLD = 0.3;

    /** 文档元数据字段名：章节ID（需先在 Dify 知识库中创建该元数据字段） */
    public static final String METADATA_KEY_CHAPTER_ID = "chapter_id";

    /** 文档元数据字段名：章节名称，如“3.2 类与对象” */
    public static final String METADATA_KEY_CHAPTER_NAME = "chapter_name";

    /** 文档元数据字段名：页码，如 “P.123-125” */
    public static final String METADATA_KEY_PAGE = "page";

    private DifyConstant() {
    }
}
