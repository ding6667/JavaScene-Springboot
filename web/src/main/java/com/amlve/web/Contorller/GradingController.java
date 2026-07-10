package com.amlve.web.Contorller;

import com.amlve.common.DTO.DifyConvertRequestDTO;
import com.amlve.common.DTO.GradingStartCommonDTO;
import com.amlve.common.DTO.GradingStartProjectDTO;
import com.amlve.common.Result;
import com.amlve.ai.Service.GradingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public Result<String> startCommon(@RequestBody GradingStartCommonDTO dto) {
        String result = gradingService.runWorkflowWithCommonFiles(dto.getFileUrls(), dto.getRubric());
        return Result.success(result);
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
