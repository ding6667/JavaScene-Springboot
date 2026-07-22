package com.amlve.file.contorller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/file")
public class FIleConversionController {


//    private final FileConversionService fileConversionService;
//    /**
//     * 转换word文件为Markdown格式
//     * mcp方法
//     * @param dto
//     * @return
//     */
//    @Deprecated
//    @GetMapping("/wordToMd")
//    public WebAsyncTask<Result<String>> wordConvertToMd(@RequestBody DifyWord2mdRequestDTO dto) {
//        return new WebAsyncTask<>(30000
//                , () -> Result.success(fileConversionService.wordConvertToMd(dto.getUploadId())));
//    }
//
//    /**
//     * 转换Markdown格式为Word格式
//     * mcp方法
//     * @return
//     */
//    @Deprecated
//    @GetMapping("/mdToWord")
//    public WebAsyncTask<Result<Map<String, String>>> mdToWord(@RequestBody Difymd2WordRequestDTO dto) {
//        return new WebAsyncTask<>(30000
//                , () -> Result.success(fileConversionService.mdConvertToWord(dto.getContent())));
//    }
}
