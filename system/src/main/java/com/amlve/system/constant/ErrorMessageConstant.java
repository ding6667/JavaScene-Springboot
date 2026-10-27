package com.amlve.system.constant;

public class ErrorMessageConstant {

    public static final String LOGIN_ERROR = "登录失败,用户名或密码错误";
    public static final String REGISTER_ERROR = "注册失败,用户名已存在";
    public static final String STANDARD_NAME_EXIST = "标准名称已存在";
    public static final String CREATE_FAILED = "创建失败";
    public static final String STANDARD_NAME_EMPTY = "标准名称不能为空";
    public static final String IS_CURRENT_EMPTY = "是否当前使用不能为空";
    /**
     * 对应 StandardConfigRequest.userId（创建人标识），原文案「创建人/来源不能为空」里的“来源”让人不知道填什么
     */
    public static final String USER_ID_EMPTY = "请填写创建人";
    public static final String STANDARD_CONTENT_EMPTY = "评分标准内容不能为空";
    public static final String CLASS_NAME_EMPTY = "班级名称不能为空";
    public static final String CLASS_CODE_EMPTY = "班级编号不能为空";
    public static final String GRADE_EMPTY = "年级不能为空";
    public static final String CLASS_CODE_EXIST = "班级编号已存在";
    public static final String CLASS_NOT_FOUND = "班级不存在";
    public static final String STANDARD_NOT_FOUND = "评分标准不存在";
    public static final String UPDATE_FAILED = "更新失败";
    public static final String DELETE_FAILED = "删除失败";
    public static final String CONFIG_KEY_EMPTY = "配置项不能为空";
    public static final String CONFIG_VALUE_EMPTY = "配置项值不能为空";
    public static final String CONFIG_NOT_FOUND = "配置项不存在";
    public static final String USER_NOT_FOUND = "用户不存在";
    public static final String USER_PERMISSION_DENIED = "权限不足，仅管理员可执行该操作";
    public static final String USER_USERNAME_EXIST = "用户名已存在";
    public static final String USER_ROLE_EMPTY = "用户角色不能为空";
    public static final String USER_ROLE_INVALID = "所选角色不在可选范围内，请重新选择";
    public static final String USER_ID_NOT_EMPTY = "用户ID不能为空";
    public static final String USER_DELETE_SELF = "不能删除当前登录账号";
    public static final String USER_PASSWORD_SIZE = "密码长度需在6~32位之间";
    public static final String USER_EMAIL_INVALID = "邮箱格式不正确";
    public static final String ADMIN_ALREADY_EXISTS = "管理员已存在，系统仅支持一名管理员";
    public static final String ADMIN_INIT_KEY_EMPTY = "管理员初始化密钥不能为空";
    public static final String ADMIN_INIT_KEY_INVALID = "管理员初始化密钥错误";

    // ==================== 角色 ====================

    /**
     * 角色相关提示。原先的「用户角色不合法，仅支持 STUDENT 或 TEACHER」把枚举名暴露给了用户，
     * 且系统实际已有第三种角色，故改为不依赖内部命名的表述
     */
    public static final String ROLE_NOT_SELECTED = "请选择角色";

    public static final String ROLE_NOT_FOUND = "所选角色不存在，请重新选择";

    public static final String ROLE_NAME_NOT_FOUND = "系统中没有这个角色";

    // ==================== 班级 ====================

    /**
     * 教师访问非自己任教班级
     */
    public static final String CLASS_NOT_OWNED = "这个班级不是你任教的班级，无法查看";

    /**
     * 学生访问非自己班级
     */
    public static final String CLASS_ONLY_OWN = "只能查看自己所在班级的学生";

    // ==================== 角色扩展信息 ====================

    public static final String STUDENT_NO_REQUIRED = "首次完善资料必须填写学号";

    public static final String STUDENT_NO_TAKEN = "这个学号已经被使用，请核对后重新填写";

    public static final String TEACHER_NO_REQUIRED = "首次完善资料必须填写工号";

    public static final String TEACHER_NO_TAKEN = "这个工号已经被使用，请核对后重新填写";

    /**
     * 三种扩展记录重复创建共用一条文案，内部表名不暴露给用户
     */
    public static final String EXT_INFO_EXISTS = "该账号的角色资料已存在，请勿重复创建";

    // ==================== 批量导入学生 ====================

    public static final String IMPORT_FORMAT_INVALID = "每行需按「学号,姓名」填写，可选追加初始密码";

    public static final String IMPORT_FIELD_EMPTY = "学号和姓名都不能为空";

    public static final String IMPORT_DUPLICATE_IN_FILE = "同一个学号在文件里出现了多次";

    public static final String IMPORT_STUDENT_NO_EXISTS = "学号已存在，该行未导入";

    public static final String IMPORT_USERNAME_EXISTS = "该学号对应的登录账号已存在，该行未导入";

    public static final String IMPORT_FILE_EMPTY = "请选择要导入的文件";

    public static final String IMPORT_OVER_LIMIT = "单次导入不能超过 200 条，超出部分未导入";

    /**
     * 读取失败时不拼 e.getMessage()，那是编码/流异常一类的技术文本，用户看不出该做什么
     */
    public static final String IMPORT_FILE_READ_FAILED = "文件读取失败，请确认是 UTF-8 编码的 txt 或 md 文件";

    // ==================== 账号自助操作 ====================

    public static final String OLD_PASSWORD_INCORRECT = "原密码不正确，请重新输入";

    /**
     * 合并原先两句：自助编辑不能改自己的启用状态，也不能切换自己的状态
     */
    public static final String SELF_STATUS_FORBIDDEN = "不能修改当前登录账号的启用状态";

    public static final String SELF_ROLE_FORBIDDEN = "不能修改当前登录账号的角色";

    // ==================== 校验注解用 ====================

    public static final String USERNAME_EMPTY = "请填写登录账号";

    public static final String REAL_NAME_EMPTY = "请填写真实姓名";

    public static final String REAL_NAME_SIZE = "姓名长度需在 1~50 之间";

    public static final String PHONE_INVALID = "手机号格式不正确";

    public static final String NEW_PASSWORD_EMPTY = "请填写新密码";

    public static final String OLD_PASSWORD_EMPTY = "请填写原密码";
}