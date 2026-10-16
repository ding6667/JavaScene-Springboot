package com.amlve.teach.mapper;

import com.amlve.teach.tableEnt.TeachHomeworkQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 作业题目快照表数据库操作，MyBatis-Plus 提供通用增删改查
 */
@Mapper
public interface TeachHomeworkQuestionMapper extends BaseMapper<TeachHomeworkQuestion> {
}
