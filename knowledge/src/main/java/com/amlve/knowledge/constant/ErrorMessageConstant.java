package com.amlve.knowledge.constant;

/**
 * knowledge 模块错误文案
 */
public class ErrorMessageConstant {

    public static final String KEYWORDS_EMPTY = "检索关键词不能为空";
    public static final String KNOWLEDGE_NOT_FOUND = "未找到相关知识";
    public static final String KNOWLEDGE_SERVICE_UNAVAILABLE = "知识库服务暂时不可用，请稍后重试";
    public static final String DATASET_ID_MISSING = "知识库ID未配置（dify.dataset-id），请联系运维配置";
    public static final String COURSE_ID_EMPTY = "课程ID不能为空";
    public static final String CHAPTER_NOT_FOUND = "章节不存在";
    public static final String CHAPTER_ID_EMPTY = "章节ID不能为空";
    public static final String DOCUMENT_NAME_EMPTY = "文档名称不能为空";
    public static final String CONTENT_EMPTY = "文档内容不能为空";
    public static final String VERIFY_CONTENT_EMPTY = "校验内容不能为空";
    public static final String UPLOAD_FILE_EMPTY = "上传文件不能为空";
    public static final String UPLOAD_FILE_TYPE_UNSUPPORTED =
            "不支持的文件类型，仅支持: TXT / MARKDOWN / PDF / HTML / DOCX / XLSX / CSV / PPTX / XML / EPUB";
    public static final String DOCUMENT_ID_EMPTY = "文档ID不能为空";
    public static final String METADATA_EMPTY = "元数据不能为空";
    public static final String PERMISSION_DENIED = "无权限执行该操作，仅教师可用";
    public static final String VERIFY_PASSED = "内容包含教材关键词，校验通过";
    public static final String VERIFY_NOT_PASSED = "内容未包含任何教材关键词";

    // ===== 知识点权重配置 =====
    public static final String WEIGHT_BATCH_EMPTY = "批量保存的权重配置列表不能为空";
    public static final String WEIGHT_POINT_NOT_FOUND = "知识点不存在";
    public static final String WEIGHT_POINT_CHAPTER_MISMATCH = "知识点不属于所选章节";
    public static final String WEIGHT_MAX_LESS_THAN_BASE = "最大出题数量不能小于基础出题数量";
    public static final String PERMISSION_DENIED_ADMIN = "无权限执行该操作，仅管理员可用";
}
