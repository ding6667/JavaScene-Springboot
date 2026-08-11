package com.amlve.grading.service.impl;

import com.amlve.grading.tableEnt.GradingTask;
import com.amlve.grading.mapper.GradingTaskMapper;
import com.amlve.grading.service.GradingTaskService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GradingTaskServiceImpl extends ServiceImpl<GradingTaskMapper, GradingTask> implements GradingTaskService {
}
