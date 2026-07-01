package com.amlve.web.Contorller;

import com.amlve.common.Constant.ErrorConstant;
import com.amlve.common.PageResult;
import com.amlve.common.Result;
import com.amlve.common.VO.UploadFileVO;

import com.amlve.file.Service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {
    private final UploadService uploadService;

    @PostMapping("/zip")
    public Result<String> UploadZip(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Result.error(ErrorConstant.FILE_IS_EMPTY);
        }
        return Result.success(uploadService.uploadZip(file));
    }

    @GetMapping("/records")
    public Result<PageResult<UploadFileVO>> records (@RequestParam(defaultValue = "1") Long pageNum
            , @RequestParam(defaultValue = "10") Long pageSize
            , @RequestParam(required = false) String fileName
            , @RequestParam(defaultValue = "false") Boolean isAsc ) throws IOException {
        return Result.success(uploadService.records(pageNum,pageSize,fileName,isAsc));
    }

}
