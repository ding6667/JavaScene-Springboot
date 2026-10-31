package com.amlve.common.constant;

public class ErrorMessageConstant {
    /**
     * token 过期
     */
    public static final String TOKEN_EXPIRED = "token 过期，请重新登录";
    /**
     * token 篡改、非法
     */
    public static final String TOKEN_INVALID = "token 篡改、非法";

    /**
     * 未授权
     */
    public static final String UNAUTHORIZED_MESSAGE = "用户未登录";
    /**
     *
     */
    public static final String UPDATE_FAILED_INVALID = "更新失败";
    /**
     * 登录失败
     */
    public static final String LOGIN_FAILED = "用户名或密码错误";
    /**
     * 登录成功
     */
    public static final String SUCCESS_MSG = "登录成功";
    /**
     * 账号被管理员禁用（sys_user.enabled = 0）
     */
    public static final String ACCOUNT_DISABLED = "账号已被禁用，请联系管理员";
    /**
     * token 已被登出或踢人操作吊销
     */
    public static final String TOKEN_REVOKED = "登录已失效，请重新登录";

    // ==================== 智能体失败提示（面向用户，禁止出现服务名/配置项/状态码） ====================

    /**
     * 代码调试智能体不可用：密钥未配置、服务不可达、工作流执行失败都归到这里，
     * 用户能做的只有稍后再试，区分内部原因只会增加困惑
     */
    public static final String AI_DEBUG_UNAVAILABLE = "AI 代码分析暂时不可用，你的运行结果不受影响，请稍后再试";

    /**
     * 代码调试智能体返回内容不可用：响应不是预期结构
     */
    public static final String AI_DEBUG_UNREADABLE = "AI 代码分析这次没有返回可用的结果，请稍后再试";

    /**
     * 评分智能体不可用
     */
    public static final String AI_GRADING_UNAVAILABLE = "AI 批改暂时不可用，本题暂不计分，请稍后重新提交";

    /**
     * 评分智能体返回内容不可用
     */
    public static final String AI_GRADING_UNREADABLE = "AI 批改这次没有返回可用的结果，请稍后重新提交";

    // ==================== 智能体通用失败提示（各 agent 模块共用） ====================

    /**
     * 智能体服务不可用：密钥未配置、网络不通、工作流执行失败都归到这里。
     * 原始 HTTP 响应体只进日志——它含服务名与状态码，用户看不懂也做不了任何操作
     */
    public static final String AGENT_UNAVAILABLE = "AI 服务暂时不可用，请稍后重试";

    /**
     * 智能体返回内容不符合约定结构
     */
    public static final String AGENT_RESULT_UNREADABLE = "AI 返回的内容无法识别，请稍后重试";

    /**
     * 知识库读写不可用（答疑、文档入库链路专用，语义比通用文案更具体）
     */
    public static final String KNOWLEDGE_BASE_UNAVAILABLE = "知识库暂时不可用，请稍后重试";

    /**
     * 未登录或登录态不可用于当前操作（含"取不到当前用户ID"这类内部异常状态，
     * 对用户统一表述为未登录，不暴露 userId 概念）
     */
    public static final String NOT_LOGGED_IN_RETRY = "请先登录后再操作";

    /**
     * 提问内容为空
     */
    public static final String QA_QUESTION_EMPTY = "请先输入想问的问题";

    /**
     * 智能体有响应但内容为空，与"服务不可用"区别在于重试同一问题可能仍失败
     */
    public static final String QA_ANSWER_EMPTY = "没有找到能回答这个问题的相关内容，换个问法试试";

    /**
     * 知识库文档被校验规则拒绝。拒绝原因只进日志，它通常是"缺少字段"一类的内部规则描述
     */
    public static final String KNOWLEDGE_UPLOAD_REJECTED = "这份文档不符合知识库的收录要求，暂时无法入库";

    /**
     * 备课等链路要求先有正文内容
     */
    public static final String MARKDOWN_CONTENT_EMPTY = "请先填写或上传教案内容";

    // ==================== 跨模块请求 DTO 校验文案 ====================

    public static final String CHAPTER_NOT_SELECTED = "请先选择章节";

    /**
     * NotNull / Min / Max 三个注解共用一句，避免「不能为空」「至少为1」「最多为20」三套说法
     */
    public static final String QUESTION_COUNT_INVALID = "出题数量需在 1~20 之间";

    public static final String QA_QUESTION_TOO_LONG = "问题内容不能超过 2000 字";

    /**
     * 会话标识不合法属于前端状态过期，对用户表述为会话失效而不是"ID 长度超限"
     */
    public static final String QA_SESSION_STALE = "当前会话已失效，请刷新页面后重新提问";

    // ==================== 题库与考试（exam 模块使用） ====================

    public static final String QUESTION_NOT_FOUND = "没有找到这道题，请刷新后重试";

    public static final String QUESTION_ID_MISSING = "请先选择题目";

    public static final String QUESTION_TYPE_UNSUPPORTED = "这道题的题型暂不支持，请联系老师确认题目配置";

    public static final String QUESTION_TYPE_INVALID = "题目类型不合法，请联系老师确认题目配置";

    public static final String DIFFICULTY_CONFIG_NOT_FOUND = "所选难度配置不存在，请重新选择";

    public static final String CHAPTER_KNOWLEDGE_NOT_CONFIGURED = "该章节还没有配置知识点，暂时无法出题";

    public static final String GRADING_RESULT_NOT_FOUND = "还没有这道题的批改结果";

    public static final String GRADING_TASK_CREATE_FAILED = "批改任务创建失败，请稍后重试";

    public static final String GRADING_TASK_RESET_FAILED = "批改任务重置失败，请稍后重试";

    public static final String WRONG_QUESTION_NOT_FOUND = "没有找到这条错题记录";

    public static final String ANSWER_LIST_MISSING = "请先作答再提交";

    public static final String HOMEWORK_CHECK_FAILED = "作业状态校验未通过，请稍后重试";

    public static final String HOMEWORK_NOT_SUBMITTABLE = "当前不能提交这份作业，请确认作业是否已发布或已过截止时间";

    // ==================== 学习与判题（study 模块使用） ====================

    public static final String ANSWER_MISSING = "请先作答再提交";

    public static final String QUESTION_OPTIONS_MISSING = "这道题的选项还没配置完整，暂时无法判分，请联系老师";

    public static final String CHOICE_ANSWER_INVALID = "选择题答案格式不正确，请填写选项字母";

    public static final String JUDGE_ANSWER_INVALID = "判断题答案只能填「对」或「错」";

    public static final String CODE_MISSING = "请先输入代码再运行";

    public static final String CODE_TOO_LONG = "代码长度超出上限（%d 字符），请精简后重试";

    public static final String ANSWER_TOO_LONG = "答案长度超出上限（%d 字符），请精简后重试";

    public static final String STANDARD_ANSWER_MISSING = "这道题还没有配置标准答案，暂时无法判分，请联系老师";

    public static final String STANDARD_ANSWER_INVALID = "这道题的标准答案格式有误，暂时无法判分，请联系老师";

    public static final String PROGRAM_TESTCASE_MISSING = "这道编程题还没有测试用例，暂时无法判分，请联系老师";

    public static final String RUN_RECORD_STORAGE_UNAVAILABLE = "运行记录暂时无法保存，请稍后重试";

    public static final String QUESTION_DETAIL_FETCH_FAILED = "获取题目信息失败，请稍后重试";

    public static final String REQUEST_PARAM_MISSING = "请求参数不完整，请刷新后重试";

    public static final String CHAPTER_QUERY_FAILED = "课程章节查询失败，请稍后重试";

    // ==================== 教学与备课（teach 模块使用） ====================

    public static final String PREPARE_MATERIAL_FETCH_FAILED = "备课素材查询失败，请稍后重试";

    public static final String PREPARE_AGENT_FAILED = "备课智能体暂时不可用，请稍后重试";

    public static final String PREPARE_GENERATE_FAILED = "备课生成失败，请稍后重试";

    public static final String PREPARE_RECORD_NOT_FOUND = "没有找到这条备课记录，或你没有访问权限";

    public static final String PREPARE_PPT_NOT_GENERATED = "这条备课记录还没有生成 PPT，请先生成";

    public static final String PREPARE_LESSON_PLAN_EMPTY = "教案内容为空，无法导出";

    public static final String PPT_FILE_EXPIRED = "文件链接已过期，请重新生成";

    public static final String PPT_FILE_READ_FAILED = "文件读取失败，请重试";

    public static final String PPT_TEMPLATE_UNAVAILABLE = "PPT 模板不存在或不可用";

    public static final String TEMPLATE_SYSTEM_READONLY = "系统内置模板不能修改";

    public static final String THEME_NOT_FOUND = "所选主题不存在，请重新选择";

    // ==================== 全局参数与框架层（GlobalExceptionHandler 使用） ====================

    /**
     * 以下四句保留参数名占位：这四个错误通常由前端联调或接口误用触发，
     * 报错方是开发者而非学生，给出具体参数名比含糊的「参数有误」更有用
     */
    public static final String PARAM_MISSING_PREFIX = "缺少必要参数: ";

    public static final String METHOD_NOT_SUPPORTED_PREFIX = "该接口不支持此请求方式: ";

    public static final String CONTENT_TYPE_UNSUPPORTED_PREFIX = "不支持的请求内容类型: ";

    public static final String ENDPOINT_NOT_FOUND_PREFIX = "接口不存在: ";

    public static final String PARAM_FORMAT_INVALID = "请求参数格式不正确，请检查后重新提交";

    public static final String FILE_SIZE_EXCEEDED = "上传文件过大，请压缩后重试";

    public static final String ACCESS_DENIED_GENERIC = "权限不足，无法执行该操作";

    public static final String SYSTEM_ERROR_RETRY = "系统繁忙，请稍后重试";

    /**
     * 补充：作业/运行记录/模板等按主键查不到的场景。ID 只进日志，不进用户消息
     */
    public static final String HOMEWORK_NOT_FOUND = "没有找到这份作业";

    public static final String HOMEWORK_QUESTION_NOT_FOUND = "没有找到这道作业题目，请刷新后重试";

    public static final String HOMEWORK_SUBMIT_SAVE_FAILED = "作业提交保存失败，请稍后重试";

    public static final String TEACH_SCOPE_MISSING = "请指定班级或年级，二选一即可";

    public static final String RUN_RECORD_NOT_FOUND = "没有找到这条运行记录";

    public static final String RUN_RECORD_NOT_OWN = "只能查看自己的运行记录";

    public static final String TEMPLATE_NOT_FOUND = "没有找到这个模板，或你没有访问权限";
}

