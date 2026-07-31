package com.amlve.ai.service.impl;

import com.amlve.ai.tableEnt.GradingTask;
import com.amlve.ai.mapper.GradingTaskMapper;
import com.amlve.ai.service.GradingTaskService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GradingTaskServiceImpl extends ServiceImpl<GradingTaskMapper, GradingTask> implements GradingTaskService {
}
