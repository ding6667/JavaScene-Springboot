package com.amlve.teach.mapper;

import com.amlve.teach.tableEnt.TeachPpt;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * PPT文件记录表数据库操作，MyBatis-Plus 提供通用增删改查
 */
@Mapper
public interface TeachPptMapper extends BaseMapper<TeachPpt> {
}
