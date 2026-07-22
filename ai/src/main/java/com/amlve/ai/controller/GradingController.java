package com.amlve.ai.controller;


import com.amlve.ai.dto.GradingStartCommonDTO;
import com.amlve.ai.dto.GradingStartProjectDTO;
import com.amlve.common.request.Result;
import com.amlve.ai.service.GradingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/grading")
@RequiredArgsConstructor
public class GradingController {

    private final GradingService gradingService;

    /**
     * 调用Dify批阅普通文件
     * 多个文件
     * @param dto 批阅参数
     * @return
     */
    @PostMapping("/startCommon")
    public Result<Void> startCommon(@RequestBody GradingStartCommonDTO dto) {
        gradingService.runWorkflowWithCommonFiles(dto.getUploadIds(), dto.getStandardConfigId());
        return Result.success(null);
    }


    /**
     * 调用Dify批阅项目文件
     * zip文件
     * @param dto 批阅参数
     * @return
     */
    @PostMapping("/startProject")
    public Result<String> startProject(@RequestBody GradingStartProjectDTO dto) {
        String result = gradingService.startBatchReviewFromZip(dto.getUploadId(), dto.getStandardConfigId());
        return Result.success(result);
    }



}
