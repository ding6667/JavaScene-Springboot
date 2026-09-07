package com.amlve.teach.service.impl;

import com.amlve.teach.dto.HomeworkPublishRequest;
import com.amlve.teach.dto.HomeworkResponse;
import com.amlve.teach.mapper.HomeworkMapper;
import com.amlve.teach.tableEnt.Homework;
import com.amlve.teach.service.TeachService;
import com.amlve.teachapi.TeachHomeworkService;
import com.amlve.teachapi.dto.HomeworkDTO;
import com.amlve.common.utils.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教师教学业务实现，同时实现 teach-api 对外接口供其他模块调用
 */
@Service
@Slf4j
public class TeachServiceImpl implements TeachService, TeachHomeworkService {

    @Resource
    private HomeworkMapper homeworkMapper;

    @Override
    public Long publishHomework(HomeworkPublishRequest request) {
        Homework homework = new Homework();
        homework.setTitle(request.getTitle());
        homework.setContent(request.getContent());
        homework.setChapterId(request.getChapterId());
        homework.setDeadline(request.getDeadline());
        homework.setTeacherId(SecurityUtils.getUserId());
        homework.setCreateTime(LocalDateTime.now());
        homeworkMapper.insert(homework);
        log.info("教师{}发布作业: {} (作业ID={})",
                homework.getTeacherId(), homework.getTitle(), homework.getId());
        return homework.getId();
    }

    @Override
    public List<HomeworkResponse> listMyHomework() {
        Long teacherId = SecurityUtils.getUserId();
        List<Homework> homeworkList = homeworkMapper.selectList(
                new LambdaQueryWrapper<Homework>()
                        .eq(Homework::getTeacherId, teacherId)
                        .orderByDesc(Homework::getCreateTime));
        return homeworkList.stream().map(homework -> {
            HomeworkResponse response = new HomeworkResponse();
            BeanUtils.copyProperties(homework, response);
            return response;
        }).toList();
    }

    @Override
    public HomeworkDTO getHomeworkById(Long homeworkId) {
        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) {
            return null;
        }
        return toDTO(homework);
    }

    @Override
    public List<HomeworkDTO> listHomeworkByTeacherId(Long teacherId) {
        return homeworkMapper.selectList(
                        new LambdaQueryWrapper<Homework>()
                                .eq(Homework::getTeacherId, teacherId)
                                .orderByDesc(Homework::getCreateTime))
                .stream().map(this::toDTO).toList();
    }

    private HomeworkDTO toDTO(Homework homework) {
        HomeworkDTO dto = new HomeworkDTO();
        BeanUtils.copyProperties(homework, dto);
        return dto;
    }
}
