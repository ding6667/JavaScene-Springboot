package com.amlve.file.contorller;

import com.amlve.file.constant.ErrorMessageConstant;
import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.file.vo.UploadFileVO;

import com.amlve.file.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {
    private final UploadService uploadService;

    /**
     * 上传zip文件
     * @param file 上传的zip文件
     * @return 上传后的文件id
     * @throws IOException
     */
    @PostMapping("/zip")
    public Result<String> UploadZip(@RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return Result.error(ErrorMessageConstant.FILE_IS_EMPTY);
        }
        return Result.success(uploadService.uploadZip(file));
    }

    /**
     * 获取上传文件列表
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param fileName 文件名查询
     * @param isAsc 是否升序
     * @return
     * @throws IOException
     */
    @GetMapping("/records")
    public Result<PageResult<UploadFileVO>> records (@RequestParam(defaultValue = "1") Long pageNum
            , @RequestParam(defaultValue = "10") Long pageSize
            , @RequestParam(required = false) String fileName
            , @RequestParam(defaultValue = "false") Boolean isAsc ) throws IOException {
        return Result.success(uploadService.records(pageNum,pageSize,fileName,isAsc));
    }

    /**
     * 删除上传文件表和存储文件
     * 1:删除成功
     * 返回错误码代表删除失败
     * @param id 文件id
     * @return
     */
    @DeleteMapping("/delete/{id}")
    public Result<Integer> delete(@PathVariable("id") String id){
        if (uploadService.delete(id) == 1){
            return Result.success(1);
        }
        return  Result.error(ErrorMessageConstant.DELETE_FAILED);
    }
}
