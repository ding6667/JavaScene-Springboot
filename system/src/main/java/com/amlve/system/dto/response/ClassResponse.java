package com.amlve.system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassResponse {

    private Long id;

    private String className;

    private String classCode;

    private Integer grade;

    private Long userId;

    private String semester;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}