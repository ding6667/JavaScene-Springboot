package com.amlve.knowledge.enumeration;

/**
 * 知识库上传记录状态
 * <p>
 * 与 knowledge_upload_record.status 列取值一致（0/1/2）。
 * 中文描述统一由本枚举提供，禁止在 Service 里写 if (status == 1) "成功" 之类的硬编码。
 * <p>
 * 未用 MyBatis-Plus 的 {@code @EnumValue} 直接映射，因为实体字段保持 Integer：
 * 查询条件、分页筛选都按数字传入，枚举只做展示层翻译，避免类型转换在 Wrapper 里出意外。
 */
public enum UploadStatusEnum {

    /** 已发起上传，索引尚未回传结果 */
    PROCESSING(0, "处理中"),

    /** 索引完成且分段数大于 0 */
    SUCCESS(1, "成功"),

    /** 索引报错、空壳或超时，异常文档已从 Dify 清理 */
    FAILED(2, "失败");

    private final Integer code;

    private final String desc;

    UploadStatusEnum(Integer code, String desc) {
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
     * 按状态码取中文描述
     *
     * @param code 状态码，可为 null
     * @return 对应描述；未匹配返回"未知"
     */
    public static String descOf(Integer code) {
        if (code != null) {
            for (UploadStatusEnum value : values()) {
                if (value.code.equals(code)) {
                    return value.desc;
                }
            }
        }
        return "未知";
    }
}
