package com.amlve.file.service.serviceImpl;

import com.amlve.file.constant.BusinessConstant;
import com.amlve.common.utils.FileFormatUtil;
import com.amlve.file.mapper.UploadMapper;
import com.amlve.file.service.FileConversionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileConversionServiceImpl implements FileConversionService {
    private final FileFormatUtil fileFormatUtil;
    private final UploadMapper uploadMapper;

    @Override
    public String wordConvertToMd(String uploadId) {
        return fileFormatUtil.word2Md(List.of(uploadMapper.selectById(uploadId).getRawStoragePath())).getOrDefault(BusinessConstant.FILES, new ArrayList<>()).get(0);
    }

    @Override
    public byte[] mdConvertToWord(String content) {
        return fileFormatUtil.generateWord(content);
    }
}
