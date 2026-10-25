create table javascene.exam_grading_task
(
    id            bigint auto_increment comment '主键ID'
        primary key,
    homework_id   bigint                             not null comment '作业ID（关联 teach_homework）',
    user_id       bigint                             not null comment '提交学生ID',
    total_score   int      default 100               null comment '作业满分',
    student_score int                                null comment '学生总分',
    score_detail  json                               null comment '单题得分与错误原因，JSON数组',
    status        tinyint  default 0                 not null comment '批改状态：0=待批改，1=已完成，2=批改失败',
    error_message varchar(2000)                      null comment '批改失败原因',
    create_time   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted       tinyint  default 0                 not null comment '逻辑删除',
    constraint uk_homework_user
        unique (homework_id, user_id)
)
    comment '作业批改任务表（按学生+作业汇总）' collate = utf8mb4_unicode_ci;

create index idx_deleted
    on javascene.exam_grading_task (deleted);

create index idx_homework_id
    on javascene.exam_grading_task (homework_id);

create index idx_user_id
    on javascene.exam_grading_task (user_id);

create table javascene.exam_homework_submit
(
    id             bigint auto_increment comment '主键ID'
        primary key,
    homework_id    bigint                             not null comment '作业ID（关联teach模块）',
    user_id        bigint                             not null comment '提交学生ID',
    question_id    bigint                             not null comment '题目ID',
    answer_content varchar(2000)                      null comment '学生答案',
    submit_time    datetime default CURRENT_TIMESTAMP not null comment '提交时间',
    create_time    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted        tinyint  default 0                 not null comment '逻辑删除',
    constraint uk_homework_user_question
        unique (homework_id, user_id, question_id)
)
    comment '作业提交表' collate = utf8mb4_unicode_ci;

create index idx_deleted
    on javascene.exam_homework_submit (deleted);

create index idx_homework_id
    on javascene.exam_homework_submit (homework_id);

create index idx_question_id
    on javascene.exam_homework_submit (question_id);

create index idx_user_id
    on javascene.exam_homework_submit (user_id);

create table javascene.exam_question
(
    id                   bigint auto_increment comment '主键ID'
        primary key,
    content              text                                    not null comment '题目内容',
    type                 tinyint                                 not null comment '题型（1=选择，2=填空，3=判断，4=程序阅读，5=编程题）',
    options              json                                    null comment '选项（JSON数组，选择题用）',
    requirements         json                                    null comment '主观题要求',
    code_framework       text                                    null comment '编程题代码框架',
    hints                json                                    null comment '阶梯提示，JSON数组',
    related_question_ids json                                    null comment '相似题ID列表',
    answer               json                                    not null comment '标准答案',
    analysis             text                                    null comment '解析',
    solution_outline     text                                    null comment '解题思路概述',
    chapter_id           bigint                                  not null comment '关联章节ID',
    difficulty           tinyint       default 1                 null comment '难度（1=基础，2=进阶，3=拔高）',
    difficulty_score     decimal(3, 1)                           null comment '原始难度分（1.0-5.0）',
    cognitive_level      varchar(20)                             null comment '认知层次',
    ability_dimensions   json                                    null comment '能力维度',
    common_mistakes      json                                    null comment '常见错误',
    knowledge_points     json                                    null,
    score                int           default 5                 null comment '默认分值',
    weight               decimal(5, 2) default 1.00              null comment '组卷权重',
    create_time          datetime      default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time          datetime      default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted              tinyint       default 0                 not null comment '逻辑删除',
    test_cases           json                                    null comment '测试用例，JSON数组',
    judge_mode           tinyint       default 1                 null comment '判题方式：1=AI批改，2=测试用例判题，3=混合',
    submit_count         int           default 0                 null comment '总提交次数',
    accepted_count       int           default 0                 null comment '通过次数',
    acceptance_rate      decimal(5, 2) default 0.00              null comment '通过率（%）'
)
    comment '题目表' collate = utf8mb4_unicode_ci;

create index idx_chapter_id
    on javascene.exam_question (chapter_id);

create index idx_deleted
    on javascene.exam_question (deleted);

create index idx_difficulty
    on javascene.exam_question (difficulty);

create index idx_type
    on javascene.exam_question (type);

create table javascene.exam_question_difficulty_config
(
    id                  bigint auto_increment
        primary key,
    code                varchar(64)                             not null comment '难度编码，如 basic/medium/hard/custom_001',
    name                varchar(64)                             not null comment '难度名称，如 简单、中等、困难',
    description         varchar(512)                            null comment '难度说明',
    example             varchar(512)                            null comment '题目示例',
    level_order         int           default 1                 not null comment '难度排序，1最简单',
    cognitive_levels    json                                    null comment '认知层次，如 ["记忆","理解"]',
    knowledge_point_min int           default 1                 not null comment '最少知识点数量',
    knowledge_point_max int           default 1                 not null comment '最多知识点数量',
    allow_cross_points  tinyint       default 0                 not null comment '是否允许跨知识点，0否1是',
    reasoning_steps_min int           default 0                 not null comment '最少推理步骤',
    reasoning_steps_max int           default 1                 not null comment '最多推理步骤',
    code_lines_min      int           default 0                 not null comment '最少代码行数',
    code_lines_max      int           default 5                 not null comment '最多代码行数',
    question_length_max int           default 80                not null comment '题干最大字数',
    option_count        int           default 4                 not null comment '选项数量',
    distractor_strength varchar(16)   default 'low'             not null comment '干扰项强度：low/medium/high',
    score_min           decimal(4, 2) default 1.00              not null comment '难度分下限',
    score_max           decimal(4, 2) default 2.00              not null comment '难度分上限',
    is_system           tinyint       default 0                 not null comment '是否系统预设',
    teacher_id          bigint                                  null comment '教师ID，NULL表示系统级,关联user表',
    chapter_id          bigint                                  null comment '绑定章节，NULL表示通用',
    status              tinyint       default 1                 not null comment '1启用 0禁用',
    sort_order          int           default 0                 not null,
    create_time         datetime      default CURRENT_TIMESTAMP not null,
    update_time         datetime      default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted             tinyint       default 0                 not null comment '是否删除',
    constraint uk_code_teacher_chapter
        unique (code, teacher_id, chapter_id)
)
    comment '出题难度配置表';

create index idx_deleted
    on javascene.exam_question_difficulty_config (deleted);

create index idx_status
    on javascene.exam_question_difficulty_config (status);

create index idx_teacher_chapter
    on javascene.exam_question_difficulty_config (teacher_id, chapter_id);

create table javascene.exam_wrong_question
(
    id              bigint auto_increment comment '主键ID'
        primary key,
    user_id         bigint                             not null comment '学生ID',
    question_id     bigint                             not null comment '题目ID',
    source          tinyint  default 1                 null comment '来源：1=作业，2=刷题',
    wrong_count     int      default 0                 not null comment '累计错误次数',
    last_answer     text                               null comment '最近一次错误答案',
    mastered        tinyint  default 0                 not null comment '是否已掌握：0=未掌握，1=已掌握',
    last_wrong_time datetime                           null comment '最近答错时间',
    create_time     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted         tinyint  default 0                 not null comment '逻辑删除',
    constraint uk_user_question
        unique (user_id, question_id)
)
    comment '错题归档表' collate = utf8mb4_unicode_ci;

create index idx_deleted
    on javascene.exam_wrong_question (deleted);

create index idx_question_id
    on javascene.exam_wrong_question (question_id);

create index idx_user_id
    on javascene.exam_wrong_question (user_id);

create table javascene.intent_draft
(
    id                  bigint auto_increment
        primary key,
    draft_intent_type   varchar(32)                        not null comment 'AI推测的意图类型',
    draft_template_text json                               not null comment 'AI生成的模板问法',
    original_user_query varchar(500)                       not null comment '触发该建议的用户原始问句',
    status              tinyint  default 0                 null comment '0-待审核, 1-已批准(已入库), 2-已拒绝',
    created_time        datetime default CURRENT_TIMESTAMP null,
    reviewed_time       datetime                           null
);

create table javascene.knowledge_chapter
(
    id               bigint                             not null comment '主键ID'
        primary key,
    course_id        bigint                             null comment '所属课程ID',
    parent_id        bigint   default 0                 null comment '父章节ID（0表示顶级章）',
    chapter_code     varchar(20)                        not null comment '章节编号（如 3.2）',
    top_chapter_code bigint                             null comment '所属大章',
    title            varchar(100)                       not null comment '章节标题',
    level            tinyint  default 1                 not null comment '层级',
    sort_order       int      default 0                 null comment '排序序号',
    description      varchar(500)                       null comment '章节简介',
    summary          text                               null,
    create_time      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted          tinyint  default 0                 not null comment '逻辑删除（0=未删，1=已删）'
)
    comment '教材章节表' collate = utf8mb4_unicode_ci;

create index idx_course_id
    on javascene.knowledge_chapter (course_id);

create index idx_delete_flag
    on javascene.knowledge_chapter (deleted);

create index idx_parent_id
    on javascene.knowledge_chapter (parent_id);

create table javascene.knowledge_code_snippet
(
    id             bigint                               not null comment '主键ID'
        primary key,
    chapter_id     bigint                               not null comment '章节ID（关联章节表）',
    title          varchar(255)                         null comment '代码段标题，如"数组的三种初始化方式"',
    code           text                                 not null comment '纯代码内容，无讲解',
    is_key_example tinyint(1) default 0                 null comment '是否为重点案例',
    page           int                                  null comment '教材页码',
    page_order     int        default 0                 not null comment '同一页内顺序（数字越小越靠前）',
    deleted        tinyint    default 0                 null comment '逻辑删除: 0-未删除, 1-已删除',
    create_time    timestamp  default CURRENT_TIMESTAMP null comment '创建时间',
    update_time    timestamp  default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '教材代码片段表';

create index idx_chapter_deleted
    on javascene.knowledge_code_snippet (chapter_id, deleted);

create index idx_chapter_key_deleted
    on javascene.knowledge_code_snippet (chapter_id, is_key_example, deleted);

create table javascene.knowledge_concept
(
    id          bigint                              not null comment '主键ID'
        primary key,
    chapter_id  bigint                              not null comment '章节ID（关联章节表）',
    name        varchar(255)                        not null comment '概念名称，如"数组的初始化"',
    definition  text                                null comment '概念定义',
    difficulty  tinyint   default 1                 null comment '难度: 1-基础, 2-进阶, 3-拔高',
    page        int                                 null comment '教材页码',
    page_order  int       default 0                 not null comment '同一页内顺序（数字越小越靠前）',
    deleted     tinyint   default 0                 null comment '逻辑删除: 0-未删除, 1-已删除',
    create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '教材核心概念表';

create index idx_chapter_deleted
    on javascene.knowledge_concept (chapter_id, deleted);

create index idx_name_deleted
    on javascene.knowledge_concept (name, deleted);

create table javascene.knowledge_content
(
    id                 bigint                             not null comment '主键ID'
        primary key,
    chapter_id         bigint                             not null comment '关联章节ID',
    knowledge_point_id bigint                             null comment '关联知识点ID（可为空）',
    content            text                               not null comment '教材原文摘录',
    content_type       tinyint  default 1                 null comment '类型（1=概念定义，2=例题，3=代码案例，4=习题）',
    page               int                                null comment '页码',
    page_order         int      default 0                 not null comment '同一页内顺序（数字越小越靠前）',
    dify_document_id   varchar(100)                       null comment 'Dify知识库文档ID（关联外部）',
    create_time        datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time        datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted            tinyint  default 0                 not null comment '逻辑删除（0=未删，1=已删）'
)
    comment '教材原文内容表（AI检索用）' collate = utf8mb4_unicode_ci;

create fulltext index ft_content
    on javascene.knowledge_content (content);

create index idx_chapter_id
    on javascene.knowledge_content (chapter_id);

create index idx_content_type
    on javascene.knowledge_content (content_type);

create index idx_deleted
    on javascene.knowledge_content (deleted);

create index idx_knowledge_point_id
    on javascene.knowledge_content (knowledge_point_id);

create table javascene.knowledge_difficult_point
(
    id          bigint                              not null comment '主键ID'
        primary key,
    chapter_id  bigint                              not null comment '章节ID（关联章节表）',
    description text                                not null comment '重难点描述，如"数组下标越界的本质"',
    type        tinyint   default 1                 null comment '类型: 1-易错, 2-高频考点, 3-概念辨析',
    page        int                                 null comment '教材页码',
    page_order  int       default 0                 not null comment '同一页内顺序（数字越小越靠前）',
    deleted     tinyint   default 0                 null comment '逻辑删除: 0-未删除, 1-已删除',
    create_time timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    update_time timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '教材重难点表';

create index idx_chapter_deleted
    on javascene.knowledge_difficult_point (chapter_id, deleted);

create index idx_chapter_type_deleted
    on javascene.knowledge_difficult_point (chapter_id, type, deleted);

create table javascene.knowledge_exercise
(
    id              bigint                              not null comment '主键ID'
        primary key,
    chapter_id      bigint                              not null comment '章节ID（关联章节表）',
    question        text                                not null comment '题目内容',
    answer          text                                null comment '标准答案',
    difficulty      tinyint   default 1                 null comment '难度: 1-基础, 2-进阶, 3-拔高',
    knowledge_point varchar(100)                        null comment '关联知识点名称',
    page            int                                 null comment '教材页码',
    page_order      int       default 0                 not null comment '同一页内顺序（数字越小越靠前）',
    deleted         tinyint   default 0                 null comment '逻辑删除: 0-未删除, 1-已删除',
    create_time     timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    update_time     timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '教材课后习题表';

create index idx_chapter_deleted
    on javascene.knowledge_exercise (chapter_id, deleted);

create index idx_chapter_difficulty_deleted
    on javascene.knowledge_exercise (chapter_id, difficulty, deleted);

create index idx_knowledge_point_deleted
    on javascene.knowledge_exercise (knowledge_point, deleted);

create table javascene.knowledge_keyword
(
    id                 bigint                             not null comment '主键ID'
        primary key,
    chapter_id         bigint                             null comment '关联章节ID（NULL表示全局关键词）',
    knowledge_point_id bigint                             null comment '关联知识点ID（可为空）',
    keyword            varchar(50)                        not null comment '关键词（如 继承、多态）',
    description        varchar(500)                       null comment '关键词释义/说明',
    page               int                                null comment '教材页码',
    page_order         int      default 0                 not null comment '同一页内顺序（数字越小越靠前）',
    synonyms           varchar(500)                       null comment '同义词（逗号分隔，如：封装,信息隐藏）',
    create_time        datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time        datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted            tinyint  default 0                 not null comment '逻辑删除（0=未删，1=已删）',
    constraint uk_keyword
        unique (keyword)
)
    comment '教材关键词表（AI校验用）' collate = utf8mb4_unicode_ci;

create index idx_chapter_id
    on javascene.knowledge_keyword (chapter_id);

create index idx_deleted
    on javascene.knowledge_keyword (deleted);

create index idx_point_id
    on javascene.knowledge_keyword (knowledge_point_id);

create table javascene.knowledge_point
(
    id          bigint                             not null comment '主键ID'
        primary key,
    chapter_id  bigint                             not null comment '关联章节ID',
    name        varchar(100)                       not null comment '知识点名称',
    description text                               null comment '知识点描述/定义（从教材摘录）',
    difficulty  tinyint  default 1                 null comment '难度（1=了解，2=理解，3=掌握，4=重难点）',
    keyword     varchar(500)                       null comment '关键词（逗号分隔，如：封装,信息隐藏）',
    page_ref    varchar(50)                        null comment '教材页码（如 P.123-125）',
    page_order  int      default 0                 not null comment '同一页内顺序（数字越小越靠前）',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted     tinyint  default 0                 not null comment '逻辑删除（0=未删，1=已删）'
)
    comment '教材知识点表' collate = utf8mb4_unicode_ci;

create fulltext index ft_keyword
    on javascene.knowledge_point (keyword);

create index idx_chapter_id
    on javascene.knowledge_point (chapter_id);

create index idx_deleted
    on javascene.knowledge_point (deleted);

create index idx_difficulty
    on javascene.knowledge_point (difficulty);

create table javascene.knowledge_upload_record
(
    id                 bigint auto_increment comment '主键ID'
        primary key,
    dataset_id         varchar(64)                        not null comment 'Dify知识库ID',
    document_id        varchar(64)                        null comment 'Dify文档ID（上传成功后填充）',
    batch              varchar(64)                        null comment 'Dify批次标识（用于查询索引状态）',
    chapter_id         bigint                             not null comment '关联章节ID',
    chapter_name       varchar(100)                       null comment '章节名称（冗余，便于查询）',
    file_name          varchar(255)                       not null comment '原始文件名',
    file_size          bigint   default 0                 null comment '文件大小（字节）',
    file_type          varchar(20)                        null comment '文件类型：txt/md/pdf/docx',
    upload_type        tinyint  default 1                 not null comment '上传类型：1=新增，2=更新',
    status             tinyint  default 0                 not null comment '状态：0=处理中，1=成功，2=失败',
    total_segments     int      default 0                 null comment '生成的分段总数',
    completed_segments int      default 0                 null comment '已完成的分段数',
    error_msg          varchar(500)                       null comment '失败原因',
    upload_by          bigint                             not null comment '上传人ID（关联 sys_user.id）',
    upload_by_name     varchar(50)                        null comment '上传人姓名（冗余）',
    upload_time        datetime default CURRENT_TIMESTAMP not null comment '上传时间',
    finish_time        datetime                           null comment '处理完成时间',
    create_time        datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time        datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted            tinyint  default 0                 not null comment '逻辑删除'
)
    comment '知识库上传记录表' collate = utf8mb4_unicode_ci;

create index idx_chapter_id
    on javascene.knowledge_upload_record (chapter_id);

create index idx_dataset_id
    on javascene.knowledge_upload_record (dataset_id);

create index idx_deleted
    on javascene.knowledge_upload_record (deleted);

create index idx_document_id
    on javascene.knowledge_upload_record (document_id);

create index idx_status
    on javascene.knowledge_upload_record (status);

create index idx_upload_by
    on javascene.knowledge_upload_record (upload_by);

create index idx_upload_time
    on javascene.knowledge_upload_record (upload_time);

create table javascene.knowledge_weight_config
(
    id                      bigint                                  not null comment '主键ID'
        primary key,
    chapter_id              bigint                                  not null comment '关联章节ID',
    knowledge_point_id      bigint                                  not null comment '关联知识点ID',
    weight                  decimal(5, 2) default 1.00              not null comment '权重值（1.00-5.00）',
    base_question_count     int           default 1                 null comment '基础出题数量',
    max_question_count      int           default 5                 null comment '最大出题数量',
    difficulty_distribution varchar(100)  default '30,50,20'        null comment '难度分布百分比',
    is_active               tinyint       default 1                 null comment '是否启用（1=启用，0=禁用）',
    create_time             datetime      default CURRENT_TIMESTAMP not null,
    update_time             datetime      default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted                 tinyint       default 0                 not null,
    constraint uk_chapter_point
        unique (chapter_id, knowledge_point_id)
)
    comment '知识点权重配置表' collate = utf8mb4_unicode_ci;

create index idx_chapter_id
    on javascene.knowledge_weight_config (chapter_id);

create index idx_weight
    on javascene.knowledge_weight_config (weight desc);

create table javascene.qa_record
(
    id                bigint auto_increment comment '主键ID'
        primary key,
    user_id           bigint                             not null comment '学生ID',
    chapter_id        bigint                             null comment '章节ID',
    session_id        varchar(64)                        not null comment '会话ID（一次完整对话的唯一标识）',
    round_no          int      default 1                 not null comment '轮次序号（同一session内从1递增）',
    question          text                               not null comment '学生问题',
    answer            text                               not null comment 'AI回答',
    knowledge_points  varchar(500)                       null comment '关联知识点（逗号分隔）',
    reference_text    text                               null comment '教材原文引用',
    is_textbook_match tinyint  default 1                 null comment '是否匹配教材：1=是，0=否（超纲）',
    dify_message_id   varchar(64)                        null comment 'Dify消息ID',
    tokens_used       int      default 0                 null comment '消耗token数',
    response_time     int      default 0                 null comment '响应耗时（毫秒）',
    create_time       datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    deleted           tinyint  default 0                 not null comment '逻辑删除'
)
    comment '学生问答记录表' collate = utf8mb4_unicode_ci;

create index idx_chapter_id
    on javascene.qa_record (chapter_id);

create index idx_create_time
    on javascene.qa_record (create_time);

create index idx_deleted
    on javascene.qa_record (deleted);

create index idx_session_id
    on javascene.qa_record (session_id);

create index idx_session_round
    on javascene.qa_record (session_id, round_no);

create index idx_user_id
    on javascene.qa_record (user_id);

create table javascene.standard_config
(
    id               bigint auto_increment
        primary key,
    standard_name    varchar(200)                       not null comment '标准名称',
    standard_content json                               not null comment '评分标准内容(Markdown)',
    is_current       tinyint  default 0                 not null comment '是否当前使用: 0=否, 1=是',
    user_id          bigint                             null comment '创建人/来源',
    create_time      datetime default CURRENT_TIMESTAMP not null,
    update_time      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted          tinyint  default 0                 not null
)
    comment '评分标准配置表';

create index idx_is_current
    on javascene.standard_config (is_current);

create table javascene.study_code_run_record
(
    id                  bigint auto_increment comment '主键ID'
        primary key,
    user_id             bigint                             not null comment '用户ID',
    chapter_id          bigint                             null comment '关联章节ID',
    question_id         bigint                             null comment '关联题目ID（NULL=自由练习）',
    code_content        text                               not null comment '用户提交的代码',
    run_result          text                               null comment '运行输出结果',
    error_msg           text                               null comment '编译/运行时错误信息',
    error_line          int                                null comment '错误行号',
    error_line_list     json                               null comment '错误行号列表（AI 返回）',
    status              tinyint  default 1                 null comment '1=成功，2=编译错误，3=运行时错误',
    judge_status        varchar(10)                        null comment 'AC/WA/TLE/MLE/RE/CE/OLE/SV/SE',
    judge_result        tinyint  default -1                null comment '1=全通过，0=部分通过，-1=未判题',
    test_result         json                               null comment '测试用例执行结果',
    passed_count        int      default 0                 null comment '通过用例数',
    total_count         int      default 0                 null comment '总用例数',
    run_duration        int      default 0                 null comment '运行耗时（毫秒）',
    memory_used         int      default 0                 null comment '内存占用（KB）',
    error_reason        varchar(1000)                      null comment '错误原因（AI 调试返回）',
    optimize_suggest    varchar(1000)                      null comment '优化建议（AI 调试返回）',
    standard_idea       varchar(1000)                      null comment '标准解题思路（AI 调试返回）',
    knowledge_points    varchar(500)                       null comment '关联知识点（逗号分隔）',
    code_quality        json                               null comment '代码质量评分',
    complexity          json                               null comment '复杂度分析',
    steps               json                               null comment '分步引导',
    common_mistakes     json                               null comment '常见错误对比',
    improvements        json                               null comment '改进建议',
    related_knowledge   json                               null comment '关联教材',
    similar_questions   json                               null comment '相似题推荐',
    encouragement       varchar(500)                       null comment '鼓励性话语',
    learning_suggestion varchar(500)                       null comment '学习建议',
    create_time         datetime default CURRENT_TIMESTAMP not null,
    update_time         datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted             tinyint  default 0                 not null
)
    comment '代码运行记录表' collate = utf8mb4_unicode_ci;

create index idx_chapter_id
    on javascene.study_code_run_record (chapter_id);

create index idx_question_id
    on javascene.study_code_run_record (question_id);

create index idx_status
    on javascene.study_code_run_record (status);

create index idx_user_id
    on javascene.study_code_run_record (user_id);

create table javascene.study_learning_progress
(
    id              bigint auto_increment comment '主键ID'
        primary key,
    user_id         bigint                             not null comment '用户ID',
    chapter_id      bigint                             not null comment '章节ID',
    progress_rate   int      default 0                 null comment '本章进度（0-100）',
    study_duration  int      default 0                 null comment '累计学习时长（秒）',
    last_study_time datetime                           null comment '最后学习时间',
    is_completed    tinyint  default 0                 null comment '是否完成',
    create_time     datetime default CURRENT_TIMESTAMP not null,
    update_time     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted         tinyint  default 0                 not null,
    constraint uk_user_chapter
        unique (user_id, chapter_id)
)
    comment '学习进度表' collate = utf8mb4_unicode_ci;

create table javascene.study_practice_record
(
    id             bigint auto_increment comment '主键ID'
        primary key,
    user_id        bigint                             not null comment '学生ID',
    question_id    bigint                             not null comment '题目ID',
    chapter_id     bigint                             null comment '章节ID',
    practice_mode  tinyint  default 1                 not null comment '练习模式：1=随机，2=章节专项，3=错题重做',
    answer_content text                               null comment '学生答案',
    duration       int      default 0                 null comment '耗时（秒）',
    attempt_count  int      default 1                 null comment '尝试次数',
    is_correct     tinyint                            null comment '是否正确：1=正确，0=错误',
    score          int                                null comment '得分（主观题部分得分时用）',
    run_record_id  bigint                             null comment '关联最后一次代码运行记录ID',
    status         tinyint  default 0                 not null comment '状态：0=草稿，1=已提交',
    submit_time    datetime default CURRENT_TIMESTAMP null comment '提交时间',
    create_time    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted        tinyint  default 0                 not null comment '逻辑删除',
    constraint uk_user_question
        unique (user_id, question_id)
)
    comment '学生自主练习记录表' collate = utf8mb4_unicode_ci;

create index idx_chapter_id
    on javascene.study_practice_record (chapter_id);

create index idx_deleted
    on javascene.study_practice_record (deleted);

create index idx_practice_mode
    on javascene.study_practice_record (practice_mode);

create index idx_run_record_id
    on javascene.study_practice_record (run_record_id);

create index idx_status
    on javascene.study_practice_record (status);

create index idx_user_id
    on javascene.study_practice_record (user_id);

create table javascene.study_question_status
(
    id               bigint auto_increment comment '主键ID'
        primary key,
    user_id          bigint                             not null comment '学生ID',
    question_id      bigint                             not null comment '题目ID',
    status           tinyint  default 0                 null comment '0=未做，1=尝试过，2=已通过',
    attempt_count    int      default 0                 null comment '尝试次数',
    first_ac_time    datetime                           null comment '首次通过时间',
    last_submit_time datetime                           null comment '最后提交时间',
    create_time      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted          tinyint  default 0                 not null comment '逻辑删除',
    constraint uk_user_question
        unique (user_id, question_id)
)
    comment '学生题目状态表' collate = utf8mb4_unicode_ci;

create index idx_deleted
    on javascene.study_question_status (deleted);

create index idx_user_status
    on javascene.study_question_status (user_id, status);

create table javascene.sys_admin_info
(
    id               bigint auto_increment comment '主键ID'
        primary key,
    user_id          bigint                               not null comment '关联 sys_user.id',
    admin_no         varchar(50)                          null comment '管理员工号',
    permission_level tinyint    default 1                 null comment '权限级别：1=普通管理员，2=超级管理员',
    department       varchar(50)                          null comment '所属部门',
    create_time      datetime   default CURRENT_TIMESTAMP not null,
    update_time      datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted          tinyint(1) default 0                 not null,
    constraint uk_user_id
        unique (user_id)
)
    comment '管理员信息扩展表' collate = utf8mb4_unicode_ci;

create table javascene.sys_permission
(
    id        bigint auto_increment
        primary key,
    perm_code varchar(100)     not null comment '权限编码（如：student:delete）',
    perm_name varchar(50)      null comment '权限名称（如：删除学生）',
    parent_id bigint default 0 null comment '父级ID（用于菜单树）',
    type      tinyint          null comment '类型：1-菜单，2-按钮/权限点',
    constraint perm_code
        unique (perm_code)
)
    comment '权限表';

create table javascene.sys_role
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    role_code   varchar(50)                        not null comment '角色编码（如：ROLE_TEACHER）',
    role_name   varchar(50)                        not null comment '角色名称（如：教师）',
    role_desc   varchar(255)                       null comment '角色描述',
    create_time datetime default CURRENT_TIMESTAMP null,
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    constraint role_code
        unique (role_code)
)
    comment '角色表';

create table javascene.sys_role_permission
(
    id            bigint auto_increment
        primary key,
    role_id       bigint not null,
    permission_id bigint not null,
    constraint uk_role_perm
        unique (role_id, permission_id)
)
    comment '角色权限关联表';

create table javascene.sys_student_info
(
    id              bigint auto_increment comment '主键ID'
        primary key,
    user_id         bigint                               not null comment '关联 sys_user.id',
    student_no      varchar(50)                          not null comment '学号（唯一）',
    class_id        bigint                               null comment '班级ID',
    class_name      varchar(50)                          null comment '班级名称（冗余）',
    grade           varchar(20)                          null comment '年级（如 2024级）',
    major           varchar(50)                          null comment '专业',
    college         varchar(50)                          null comment '学院',
    enroll_year     int                                  null comment '入学年份',
    graduation_year int                                  null comment '毕业年份（如 2028）',
    create_time     datetime   default CURRENT_TIMESTAMP not null,
    update_time     datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted         tinyint(1) default 0                 not null,
    constraint uk_student_no
        unique (student_no),
    constraint uk_user_id
        unique (user_id)
)
    comment '学生信息扩展表' collate = utf8mb4_unicode_ci;

create index idx_class_id
    on javascene.sys_student_info (class_id);

create table javascene.sys_teacher_info
(
    id            bigint auto_increment comment '主键ID'
        primary key,
    user_id       bigint                               not null comment '关联 sys_user.id',
    teacher_no    varchar(50)                          not null comment '工号（唯一）',
    title         varchar(20)                          null comment '职称（讲师/副教授/教授）',
    college       varchar(50)                          null comment '所属学院',
    department    varchar(50)                          null comment '所属系/教研室',
    office        varchar(50)                          null comment '办公室',
    research_area varchar(200)                         null comment '研究方向',
    create_time   datetime   default CURRENT_TIMESTAMP not null,
    update_time   datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted       tinyint(1) default 0                 not null,
    constraint uk_teacher_no
        unique (teacher_no),
    constraint uk_user_id
        unique (user_id)
)
    comment '教师信息扩展表' collate = utf8mb4_unicode_ci;

create table javascene.sys_user
(
    id              bigint auto_increment comment '主键ID'
        primary key,
    username        varchar(50)                          not null comment '登录账号（唯一）',
    password        varchar(255)                         not null comment '密码（BCrypt加密）',
    real_name       varchar(50)                          null comment '真实姓名（教师姓名）',
    email           varchar(100)                         null comment '邮箱（用于找回密码）',
    phone           varchar(20)                          null comment '手机号',
    avatar          varchar(255)                         null comment '头像URL',
    enabled         tinyint(1) default 1                 null comment '账号状态：0-禁用，1-启用',
    last_login_time datetime                             null comment '最后登录时间',
    last_login_ip   varchar(50)                          null comment '最后登录IP',
    deleted         tinyint(1) default 0                 null comment '逻辑删除（0-未删，1-已删）',
    create_time     datetime   default CURRENT_TIMESTAMP null comment '创建时间',
    update_time     datetime   default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    role_id         int                                  not null,
    role_name       varchar(10)                          null,
    constraint username
        unique (username)
)
    comment '用户表';

create index idx_username
    on javascene.sys_user (username);

create table javascene.sys_user_role
(
    id          bigint auto_increment
        primary key,
    user_id     bigint                             not null comment '用户ID（外键）',
    role_id     bigint                             not null comment '角色ID（外键）',
    create_time datetime default CURRENT_TIMESTAMP null,
    constraint uk_user_role
        unique (user_id, role_id) comment '防止重复关联'
)
    comment '用户角色关联表';

create index idx_user_id
    on javascene.sys_user_role (user_id);

create table javascene.teach_homework
(
    id                bigint auto_increment comment '主键ID'
        primary key,
    title             varchar(100)                       not null comment '作业标题',
    content           text                               not null comment '作业内容/描述',
    chapter_id        bigint                             not null comment '关联章节ID',
    deadline          datetime                           not null comment '截止时间',
    allow_late_submit tinyint  default 0                 not null comment '是否允许迟交：0否 1是',
    teacher_id        bigint                             not null comment '发布教师ID',
    class_id          bigint                             null comment '发布班级ID（NULL表示全年级）',
    grade             varchar(20)                        null comment '年级（class_id为空时表示全年级作业）',
    status            tinyint  default 1                 not null comment '状态（1=未发布，2=已发布，3=已截止，4=已归档）',
    publish_time      datetime                           null comment '发布时间',
    close_time        datetime                           null comment '关闭时间',
    close_reason      varchar(200)                       null comment '关闭原因',
    total_score       int      default 0                 not null comment '作业总分',
    question_count    int      default 0                 not null comment '题目数量',
    submitted_count   int      default 0                 not null comment '提交数量',
    create_time       datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time       datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted           tinyint  default 0                 not null comment '逻辑删除'
)
    comment '作业表' collate = utf8mb4_unicode_ci;

create index idx_chapter_id
    on javascene.teach_homework (chapter_id);

create index idx_class_id
    on javascene.teach_homework (class_id);

create index idx_deadline
    on javascene.teach_homework (deadline);

create index idx_deleted
    on javascene.teach_homework (deleted);

create index idx_status
    on javascene.teach_homework (status);

create index idx_teacher_id
    on javascene.teach_homework (teacher_id);

create table javascene.teach_homework_question
(
    id               bigint auto_increment comment '主键ID'
        primary key,
    homework_id      bigint                             not null comment '作业ID',
    question_id      bigint                             not null comment '题目ID（关联 exam_question）',
    source_type      tinyint                            null comment '来源：1教材习题生成 2AI生成 3人工录入',
    content          text                               null comment '题干快照（发布时固化）',
    type             tinyint                            null comment '题型：1选择 2填空 3判断 4程序阅读 5编程 6简答',
    options          json                               null comment '选项快照（选择题用）',
    answer           json                               null comment '标准答案快照（教师可见，学生端不返回）',
    analysis         text                               null comment '解析快照',
    difficulty       tinyint                            null comment '难度：1基础 2进阶 3拔高',
    chapter_id       bigint                             null comment '章节ID快照',
    knowledge_points json                               null comment '知识点快照',
    default_score    int                                null comment '题目默认分值（快照，用于score为空时兜底）',
    page             int                                null comment '教材页码（教材题快照）',
    sort_order       int      default 0                 null comment '题目排序',
    score            int                                null comment '本题分值（NULL则使用题目默认分值）',
    create_time      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted          tinyint  default 0                 not null comment '逻辑删除',
    constraint uk_homework_question
        unique (homework_id, question_id)
)
    comment '作业题目关联表' collate = utf8mb4_unicode_ci;

create index idx_deleted
    on javascene.teach_homework_question (deleted);

create index idx_homework_id
    on javascene.teach_homework_question (homework_id);

create index idx_question_id
    on javascene.teach_homework_question (question_id);

create table javascene.teach_ppt
(
    id             bigint auto_increment comment '主键ID'
        primary key,
    chapter_id     bigint                             not null comment '章节ID',
    file_name      varchar(255)                       not null comment '文件名（含UUID）',
    file_path      varchar(500)                       not null comment '文件存储路径',
    md_content     longtext                           not null comment 'PPT原始Markdown内容',
    file_size      bigint   default 0                 null comment '文件大小（字节）',
    download_count int      default 0                 null comment '下载次数',
    create_time    datetime default CURRENT_TIMESTAMP not null,
    update_time    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted        tinyint  default 0                 not null
)
    comment 'PPT文件记录表' collate = utf8mb4_unicode_ci;

create index idx_chapter_id
    on javascene.teach_ppt (chapter_id);

create index idx_deleted
    on javascene.teach_ppt (deleted);

create table javascene.teach_ppt_template
(
    id            bigint auto_increment comment '主键ID'
        primary key,
    name          varchar(100)                       not null comment '模板名称',
    description   varchar(500)                       null comment '模板说明',
    theme_code    varchar(50)                        not null comment '视觉主题编码（对应 CSS 文件名）',
    design_tokens json                               null comment '设计令牌（颜色、字体等）',
    structure_md  text                               not null comment '内容结构（MD 骨架）',
    preview_image varchar(255)                       null comment '预览图URL',
    is_system     tinyint  default 0                 not null comment '是否系统预设',
    teacher_id    bigint                             null comment '教师ID（自定义模板）',
    is_active     tinyint  default 1                 not null comment '是否启用',
    sort_order    int      default 0                 null comment '排序',
    create_time   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    deleted       tinyint  default 0                 not null comment '逻辑删除'
)
    comment 'PPT模板表' collate = utf8mb4_unicode_ci;

create index idx_is_system
    on javascene.teach_ppt_template (is_system);

create index idx_teacher_id
    on javascene.teach_ppt_template (teacher_id);

create table javascene.teach_prepare_record
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    chapter_id  bigint                             not null comment '章节ID',
    teacher_id  bigint                             not null comment '教师ID',
    lesson_plan longtext                           null comment '教案内容（Markdown）',
    key_points  longtext                           null comment '重难点清单（Markdown）',
    homework    longtext                           null comment '课后习题（Markdown）',
    lab         longtext                           null comment '上机实训方案（Markdown）',
    pptx_md     longtext                           null comment 'PPT原始Markdown内容',
    pptx_url    varchar(500)                       null comment 'PPT文件路径',
    status      tinyint  default 1                 null comment '状态（1=成功，0=失败）',
    error_msg   varchar(500)                       null comment '错误信息',
    create_time datetime default CURRENT_TIMESTAMP not null,
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted     tinyint  default 0                 not null
)
    comment '备课记录表' collate = utf8mb4_unicode_ci;

create index idx_chapter_id
    on javascene.teach_prepare_record (chapter_id);

create index idx_deleted
    on javascene.teach_prepare_record (deleted);

create index idx_teacher_id
    on javascene.teach_prepare_record (teacher_id);

create table javascene.user_config
(
    id           bigint auto_increment
        primary key,
    user_id      bigint                             not null comment '用户ID（或团队ID）',
    config_key   int                                not null comment '配置项键',
    config_value varchar(255)                       not null comment '配置值（JSON）',
    update_time  datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    deleted      tinyint  default 0                 not null,
    constraint uk_user_key
        unique (user_id, config_key)
);

