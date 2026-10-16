package com.amlve.knowledge.enumeration;

/**
 * 知识库上传类型
 * <p>
 * 与 knowledge_upload_record.upload_type 列取值一致（1=新增，2=更新）。
 * Dify 对同名文档是幂等的（重复上传返回同一 documentId），因此"更新"在 Dify 侧
 * 表现为覆盖，本字段仅用于留痕区分操作意图。
 */
public enum UploadTypeEnum {

    /** 首次上传 */
    CREATE(1, "新增"),

    /** 覆盖同名文档 */
    UPDATE(2, "更新");

    private final Integer code;

    private final String desc;

    UploadTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 按类型码取中文描述
     *
     * @param code 类型码，可为 null
     * @return 对应描述；未匹配返回"未知"
     */
    public static String descOf(Integer code) {
        if (code != null) {
            for (UploadTypeEnum value : values()) {
                if (value.code.equals(code)) {
                    return value.desc;
                }
            }
        }
        return "未知";
    }
}
