package com.amlve.system.service.impl;

import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.PageResult;
import com.amlve.system.constant.ErrorMessageConstant;
import com.amlve.system.converter.ClassConverter;
import com.amlve.system.dto.ClassRequest;
import com.amlve.system.mapper.ClassMapper;
import com.amlve.system.service.ClassService;
import com.amlve.system.tableEnt.ClassInfo;
import com.amlve.system.vo.ClassResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClassServiceImpl extends ServiceImpl<ClassMapper, ClassInfo> implements ClassService {

    private final ClassMapper classMapper;
    private final ClassConverter classConverter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassResponse create(ClassRequest request) {
        LambdaQueryWrapper<ClassInfo> queryWrapper = Wrappers.<ClassInfo>lambdaQuery()
                .eq(ClassInfo::getClassCode, request.getClassCode());
        ClassInfo exist = classMapper.selectOne(queryWrapper);
        if (exist != null) {
            throw new BusinessException(ErrorMessageConstant.CLASS_CODE_EXIST);
        }
        ClassInfo classInfo = classConverter.toEntity(request);
        int rows = classMapper.insert(classInfo);
        if (rows != 1) {
            throw new BusinessException(ErrorMessageConstant.CREATE_FAILED);
        }
        return classConverter.toResponse(classInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassResponse update(ClassRequest request) {
        ClassInfo exist = classMapper.selectById(request.getId());
        if (exist == null) {
            throw new BusinessException(ErrorMessageConstant.CLASS_NOT_FOUND);
        }
        LambdaQueryWrapper<ClassInfo> queryWrapper = Wrappers.<ClassInfo>lambdaQuery()
                .eq(ClassInfo::getClassCode, request.getClassCode())
                .ne(ClassInfo::getId, request.getId());
        ClassInfo duplicate = classMapper.selectOne(queryWrapper);
        if (duplicate != null) {
            throw new BusinessException(ErrorMessageConstant.CLASS_CODE_EXIST);
        }
        ClassInfo classInfo = classConverter.toEntity(request);
        classMapper.updateById(classInfo);
        ClassInfo updated = classMapper.selectById(request.getId());
        return classConverter.toResponse(updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ClassInfo exist = classMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorMessageConstant.CLASS_NOT_FOUND);
        }
        classMapper.deleteById(id);
    }

    @Override
    public ClassResponse getById(Long id) {
        ClassInfo classInfo = classMapper.selectById(id);
        if (classInfo == null) {
            throw new BusinessException(ErrorMessageConstant.CLASS_NOT_FOUND);
        }
        return classConverter.toResponse(classInfo);
    }

    @Override
    public PageResult<ClassResponse> records(Long pageNum, Long pageSize, String className, Boolean isAsc) {
        Page<ClassInfo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ClassInfo> wrapper = Wrappers.<ClassInfo>lambdaQuery();
        wrapper.like(className != null && !className.isBlank(), ClassInfo::getClassName, className);
        if (isAsc) {
            wrapper.orderByAsc(ClassInfo::getClassName);
        } else {
            wrapper.orderByDesc(ClassInfo::getClassName);
        }
        IPage<ClassInfo> pageData = classMapper.selectPage(page, wrapper);
        List<ClassResponse> voList = pageData.getRecords().stream()
                .map(classConverter::toResponse)
                .collect(Collectors.toList());
        PageResult<ClassResponse> result = new PageResult<>();
        result.setRecords(voList);
        result.setTotal(pageData.getTotal());
        result.setCurrent(pageData.getCurrent());
        result.setSize(pageData.getSize());
        return result;
    }
}