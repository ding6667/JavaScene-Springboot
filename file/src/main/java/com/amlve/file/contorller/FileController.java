package com.amlve.file.contorller;

import com.amlve.file.constant.ErrorMessageConstant;
import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.file.service.StorageService;
import com.amlve.file.tableEnt.UploadFile;
import com.amlve.file.dto.response.UploadFileVO;

import com.amlve.file.service.UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileController {
    private final UploadService uploadService;

    /**
     * 上传任意文件
     * @param file 上传的文件
     * @return 上传后的文件id
     * @throws IOException
     */
    @PostMapping("/file")
    public Result<String> UploadFile(@RequestParam(value = "file", required = false) MultipartFile file, @RequestParam(required = false) String fileType) throws IOException {
        if (file == null || file.isEmpty()) {
            return Result.error(ErrorMessageConstant.FILE_IS_EMPTY);
        }else if(file.getSize() <= 0){
            return Result.error(ErrorMessageConstant.FILE_SIZE_INVALID);
        }else if(Objects.requireNonNull(file.getOriginalFilename()).isEmpty()){
            return Result.error(ErrorMessageConstant.FILE_NAME_INVALID);
        }
        return Result.success(uploadService.uploadFile(file, fileType));
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
            , @RequestParam(defaultValue = "false") Boolean isAsc
            , @RequestParam(required = false) String fileType) throws IOException {
        return Result.success(uploadService.records(pageNum,pageSize,fileName,isAsc,fileType));
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

    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated() and @fileAuth.isOwner(#id, authentication)")
    public ResponseEntity<Resource> downloadFile(@PathVariable String id) { // 从 Token 中解析的用户ID
        Map<String, Object> resourceMap = uploadService.loadAsResource(id);
        Resource resource = (Resource) resourceMap.get("resource");
        UploadFile fileInfo = (UploadFile) resourceMap.get("fileInfo");

        String encodedFileName = URLEncoder.encode(fileInfo.getFileName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFileName +
                                "\"; filename*=UTF-8''" + encodedFileName)
                .body(resource);
    }
}