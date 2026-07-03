package com.amlve.common.Entities;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileInput {
    private String type;           // document, image, audio, video, custom
    private String transferMethod; // remote_url 或 local_file
    private String url;            // 当 transferMethod = remote_url 时
    private String uploadFileId;   // 当 transferMethod = local_file 时
}
