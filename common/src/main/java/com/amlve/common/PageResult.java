package com.amlve.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import org.apache.poi.ss.formula.functions.T;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records; // 数据列表
    private long total;     // 总条数
    private long current;   // 当前页
    private long size;      // 每页条数

    public static <T> PageResult<T> convert(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getRecords());
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        return result;
    }
}
