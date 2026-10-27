package com.amlve.common.request;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records; // 数据列表
    private long total;     // 总条数
    private long current;   // 当前页
    private long size;      // 每页条数

    /**
     * 组装分页结果，替代逐处搬运四个字段的样板。
     * 入参用基础类型而不是 MyBatis-Plus 的 IPage：common 只依赖 mybatis-plus-annotation，
     * 且 IPage 带 pages/orders/optimizeCountSql 等内部字段，本就不该进对外契约
     */
    public static <T> PageResult<T> of(long total, long current, long size, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setCurrent(current);
        result.setSize(size);
        return result;
    }

    /**
     * 空分页：用于「权限范围内无数据」，让前端少一个 null 分支
     */
    public static <T> PageResult<T> empty(long current, long size) {
        return of(0, current, size, List.of());
    }
}
