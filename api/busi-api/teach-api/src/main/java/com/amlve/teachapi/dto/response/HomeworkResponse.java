package com.amlve.teachapi.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作业信息传输对象（供其他模块跨模块调用使用，不直接暴露实体类）
 */
@Data
public class HomeworkResponse {

    private Long id;
    private String title;
    private String content;
    private Long chapterId;
    private LocalDateTime deadline;
    private Long teacherId;
    private LocalDateTime createTime;


}
