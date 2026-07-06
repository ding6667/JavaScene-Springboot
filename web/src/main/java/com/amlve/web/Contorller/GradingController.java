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
@RequestMapping("/marking")
@RequiredArgsConstructor
public class GradingController {

    private final GradingService gradingService;


    /**
     * 转换word文件为Markdown格式
     * mcp方法
     * TODO 后续整理到file模块
     * @param dto
     * @return
     */
    @Deprecated
    @GetMapping("/convert")
    public Result<Map<String, List<String>>> wordConvertToMd(@RequestBody DifyConvertRequestDTO dto) {
        log.info("dify传来待转换文件{}, way={}",dto.getFileUrl(), dto.getWay());
        Map<String, List<String>> mdFiles = gradingService.wordConvertToMd(List.of(dto.getFileUrl()));
        return Result.success(mdFiles);
    }

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
        String result = gradingService.runWorkflowWithProjectZip(dto.getId(), dto.getRubric());
        return Result.success(result);
    }

    /**
     * 转换Markdown格式为Word格式
     * TODO 后续整理到file模块
     * @return
     */
    @Deprecated
    @GetMapping("/mdToWord")
    public Result<String> mdToWord() {
        return null;
    }

}
