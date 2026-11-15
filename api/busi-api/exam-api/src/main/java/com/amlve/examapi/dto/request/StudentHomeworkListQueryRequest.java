package com.amlve.examapi.dto.request;

import lombok.Data;

/**
 * 学生「我的作业」列表查询请求。
 * <p>
 * 班级与年级不由前端传：后端按当前登录学生档案（sys_student_info）解析，
 * 避免学生越权查看其他班级作业。
 */
@Data
public class StudentHomeworkListQueryRequest {

    /**
     * 作业状态：2=已发布，3=已截止（可选，不传表示两者都查）
     */
    private Integer status;

    /**
     * 章节ID（可选，按章节筛作业）
     */
    private Long chapterId;

    /**
     * 是否只看未截止的（可选）
     */
    private Boolean onlyNotExpired;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
