package com.amlve.teach.service.impl;

import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.request.Result;
import com.amlve.teach.dto.request.HomeworkPublishRequest;
import com.amlve.teach.mapper.HomeworkMapper;
import com.amlve.teach.tableEnt.Homework;
import com.amlve.teach.service.TeachService;
import com.amlve.teachapi.service.TeachHomeworkService;
import com.amlve.teachapi.dto.response.HomeworkResponse;
import com.amlve.common.utils.SecurityUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

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
        // 发布即已发布状态（1=未发布，2=已发布）；createTime/updateTime 由 MetaHandler 自动填充
        homework.setStatus(2);
        homeworkMapper.insert(homework);
        log.info("教师{}发布作业: {} (作业ID={})",
                homework.getTeacherId(), homework.getTitle(), homework.getId());
        return homework.getId();
    }

    @Override
    public List<com.amlve.teach.dto.response.HomeworkResponse> listMyHomework() {
        Long teacherId = SecurityUtils.getUserId();
        List<Homework> homeworkList = homeworkMapper.selectList(
                new LambdaQueryWrapper<Homework>()
                        .eq(Homework::getTeacherId, teacherId)
                        .orderByDesc(Homework::getCreateTime));
        return homeworkList.stream().map(homework -> {
            com.amlve.teach.dto.response.HomeworkResponse response = new com.amlve.teach.dto.response.HomeworkResponse();
            BeanUtils.copyProperties(homework, response);
            return response;
        }).toList();
    }

    @Override
    public Result<HomeworkDTO> getHomeworkById(Long homeworkId) {
        Homework homework = homeworkMapper.selectById(homeworkId);
        // 作业不存在时返回 NOT_FOUND 错误码，调用方通过 code 判断而非判空
        if (homework == null) {
            return Result.error(ErrorCodeConstant.NOT_FOUND, "作业不存在: id=" + homeworkId);
        }
        return Result.success(toDTO(homework));
    }

    @Override
    public Result<List<HomeworkDTO>> listHomeworkByTeacherId(Long teacherId) {
        // 教师无已发布作业返回空列表，属正常业务结果，不返回 error
        return Result.success(homeworkMapper.selectList(
                        new LambdaQueryWrapper<Homework>()
                                .eq(Homework::getTeacherId, teacherId)
                                .orderByDesc(Homework::getCreateTime))
                .stream().map(this::toDTO).toList());
    }

    private HomeworkResponse toDTO(Homework homework) {
        HomeworkResponse dto = new HomeworkResponse();
        BeanUtils.copyProperties(homework, dto);
        return dto;
    }
}
