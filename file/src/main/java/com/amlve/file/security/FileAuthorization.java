package com.amlve.file.security;

import com.amlve.common.security.CustomUserDetails;
import com.amlve.common.utils.SecurityUtils;
import com.amlve.fileapi.service.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("fileAuth")
@RequiredArgsConstructor
public class FileAuthorization {

    private final UploadService uploadService; // 你的文件业务服务

    /**
     * 判断当前登录用户是否是指定文件的拥有者
     *
     * @param fileId 文件ID（从 Controller 方法参数 #id 传入）
     * @param authentication 当前安全上下文中的认证信息（自动注入）
     * @return true 表示有权限下载
     */
    public boolean isOwner(String fileId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        Long currentUserID = SecurityUtils.getUserId();

        Long fileOwnerId = uploadService.getFileOwnerId(fileId);
        if (fileOwnerId == null) {
            return false;
        }

        return fileOwnerId.equals(currentUserID);
    }
}