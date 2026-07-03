package com.amlve.web.Contorller;

import com.amlve.common.DTO.DifyConvertRequestDTO;
import com.amlve.common.DTO.MarkRequestDTO;
import com.amlve.common.Result;
import com.amlve.ai.Service.MarkingDifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/marking")
@RequiredArgsConstructor
public class DifyContorller {

    private final MarkingDifyService markingDifyService;


    /**
     * 转换word文件为Markdown格式
     * mcp方法
     *TODO
     * @param dto
     * @return
     */
    @Deprecated
    @GetMapping("/convert")
    public Result<Map<String, List<String>>> convertToMd(@RequestBody DifyConvertRequestDTO dto) {
        log.info("dify传来待转换文件{}, way={}",dto.getFileUrl(), dto.getWay());
        Map<String, List<String>> mdFiles = markingDifyService.wordConvertToMd(List.of(dto.getFileUrl()));
        return Result.success(mdFiles);
    }

    /**
     * 调用Dify批阅
     * @param dto
     * @return
     */
    @PostMapping("/start")
    public Result<String>  mark(@RequestBody MarkRequestDTO dto) {
        log.info("dify传来待批阅文件{}, rubric={}",dto.getFileUrls(), dto.getRubric());
        String result = markingDifyService.runWorkflowWithCommonFiles(dto.getFileUrls(), dto.getRubric());
        log.info("dify批阅结果路径{}", result);
        return Result.success(result);
    }



    /**
     * 转换Markdown格式为Word格式
     * TODO
     * @return
     */
    @Deprecated
    @GetMapping("/mdToWord")
    public Result<String> mdToWord() {
        return null;
    }

}
