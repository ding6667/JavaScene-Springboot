package com.amlve.study.mapper;

import com.amlve.study.tableEnt.StudyLearningProgress;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public  interface StudyProgressMapper extends BaseMapper<StudyLearningProgress> {
}
