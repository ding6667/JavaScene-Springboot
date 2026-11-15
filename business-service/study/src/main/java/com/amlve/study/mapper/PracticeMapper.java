package com.amlve.study.mapper;

import com.amlve.study.tableEnt.StudyPracticeRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PracticeMapper extends BaseMapper<StudyPracticeRecord> {
}
