package com.amlve.web.Contorller;

import com.amlve.common.DTO.DifyConvertRequestDTO;
import com.amlve.common.Result;
import com.amlve.common.Utils.FileFormatUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/file")
public class FIleConversionController {

    private final FileFormatUtil fileFormatUtil;

    /**
     * 转换word文件为Markdown格式
     * TODO: 实现转换逻辑
     * mcp方法
     * @param dto
     * @return
     */
    @Deprecated
    @GetMapping("/wordToMd")
    public Result<Map<String, List<String>>> wordConvertToMd(@RequestBody DifyConvertRequestDTO dto) {
        log.info("dify传来待转换文件{}",dto.getFileId());
        Map<String, List<String>> mdFiles = fileFormatUtil.wordConvertToMd(List.of(dto.getFileId()));
        return Result.success(mdFiles);
    }

    /**
     * 转换Markdown格式为Word格式
     * TODO: 实现转换逻辑
     * @return
     */
    @Deprecated
    @GetMapping("/mdToWord")
    public Result<String> mdToWord(@RequestBody DifyConvertRequestDTO dto) {

        return Result.success("");
    }
}
