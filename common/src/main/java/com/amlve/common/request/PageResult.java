package com.amlve.common.request;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records; // 数据列表
    private long total;     // 总条数
    private long current;   // 当前页
    private long size;      // 每页条数


}
