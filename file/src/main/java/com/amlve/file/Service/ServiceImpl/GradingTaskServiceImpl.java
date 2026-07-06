package com.amlve.file.Service.ServiceImpl;

import com.amlve.common.Entities.TableEnt.GradingTask;
import com.amlve.file.Mapper.GradingTaskMapper;
import com.amlve.file.Service.GradingTaskService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GradingTaskServiceImpl extends ServiceImpl<GradingTaskMapper, GradingTask> implements GradingTaskService {
}
