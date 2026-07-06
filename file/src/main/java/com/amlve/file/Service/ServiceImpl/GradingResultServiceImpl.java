package com.amlve.file.Service.ServiceImpl;

import com.amlve.common.Entities.TableEnt.GradingResult;
import com.amlve.file.Mapper.GradingResultMapper;
import com.amlve.file.Service.GradingResultService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GradingResultServiceImpl extends ServiceImpl<GradingResultMapper, GradingResult> implements GradingResultService {
}
