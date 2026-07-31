package com.amlve.system.constant;

public class ErrorCodeConstant {
    // ===== 通用错误码（1000-1999） =====

    /**
     * 系统内部错误（未捕获的异常）
     * 例如：空指针异常、数据库连接失败等
     */
    public static final int SYSTEM_ERROR = 1000;

    /**
     * 业务操作成功（和 HTTP 200 不同，这是业务层面的成功）
     * 注意：业务成功时也应该返回 HTTP 200
     */
    public static final int SUCCESS = 200;

    /**
     * 登录失败
     * 例如：用户名或密码错误
     */
    public static final int LOGIN_ERROR = 1007;
    /**
     * 注册失败
     * 例如：用户名已存在
     */
    public static final int REGISTER_ERROR = 1008;
    // ===== 文件相关错误码（2000-2099） =====

    /**
     * 上传的文件为空（用户选择了空文件或未选择文件）
     * 用于 @RequestParam("file") MultipartFile file 的校验
     */
    public static final int FILE_IS_EMPTY = 2000;

    /**
     * 文件 IO 操作失败
     * 例如：文件保存失败、读取失败、删除失败、磁盘空间不足
     */
    public static final int FILE_IO_ERROR = 2001;

    /**
     * 上传的文件大小超过限制
     * 配合 spring.servlet.multipart.max-file-size 配置使用
     */
    public static final int FILE_SIZE_EXCEEDED = 2002;

    /**
     * 文件不存在（业务层面）
     * 例如：根据 storage_path 查找文件时发现磁盘文件已被删除
     * 注意：和 NOT_FOUND（1003）的区别在于，这个专门针对文件
     */
    public static final int FILE_NOT_FOUND = 2003;


    // ===== 业务错误码（3000-3999） =====

    /**
     * 通用业务错误（兜底）
     * 当没有更具体的业务错误码时使用
     * 例如：订单状态不允许取消、余额不足
     */
    public static final int BUSINESS_ERROR = 3000;

    //未知错误
    public static final int UNKNOWN_ERROR = 3001;
}
