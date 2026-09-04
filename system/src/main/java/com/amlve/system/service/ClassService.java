package com.amlve.system.service;

import com.amlve.common.request.PageResult;
import com.amlve.system.dto.request.ClassRequest;
import com.amlve.system.dto.response.ClassResponse;

public interface ClassService {

    ClassResponse create(ClassRequest request);

    ClassResponse update(ClassRequest request);

    void delete(Long id);

    ClassResponse getById(Long id);

    PageResult<ClassResponse> records(Long pageNum, Long pageSize, String className, Boolean isAsc);
}