package com.amlve.grading.service.impl;

import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.PageResult;
import com.amlve.fileapi.dto.UploadFileDTO;
import com.amlve.fileapi.service.UploadService;
import com.amlve.grading.constant.ErrorCodeConstant;
import com.amlve.grading.constant.ErrorMessageConstant;
import com.amlve.grading.enumeration.FileType;
import com.amlve.grading.enumeration.GradingTaskStatus;
import com.amlve.grading.enumeration.TaskCategory;
import com.amlve.grading.mapper.GradingTaskMapper;
import com.amlve.grading.service.GradingTaskService;
import com.amlve.grading.tableEnt.GradingTask;
import com.amlve.grading.vo.GradingTaskResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GradingTaskServiceImpl extends ServiceImpl<GradingTaskMapper, GradingTask> implements GradingTaskService {

    private final UploadService uploadService;

    @Override
    public PageResult<GradingTaskResponse> pageZipTasks(Long userId, String semester, String taskCategory,
                                                         Integer taskSequence, String classId, Integer status,
                                                         Integer page, Integer size, String sortOrder) {
        LambdaQueryWrapper<GradingTask> wrapper = Wrappers.<GradingTask>lambdaQuery()
                .eq(GradingTask::getUserId, userId)
                .eq(GradingTask::getFileType, FileType.ZIP)
                .eq(semester != null && !semester.isEmpty(), GradingTask::getSemester, semester)
                .eq(taskCategory != null && !taskCategory.isEmpty(), GradingTask::getTaskCategory, TaskCategory.fromCode(taskCategory))
                .eq(taskSequence != null, GradingTask::getTaskSequence, taskSequence)
                .eq(classId != null && !classId.isEmpty(), GradingTask::getClassId, classId)
                .eq(status != null, GradingTask::getStatus, GradingTaskStatus.getValue(status));

        boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
        wrapper.orderBy(true, isAsc, GradingTask::getCreateTime);

        Page<GradingTask> pageResult = baseMapper.selectPage(new Page<>(page, size), wrapper);
        List<String> fileIds = pageResult.getRecords().stream()
                .map(GradingTask::getFileId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<String, String> fileNameMap = new HashMap<>();
        for (String id : fileIds) {
            UploadFileDTO f = uploadService.selectById(id);
            fileNameMap.put(id, f != null ? f.getFileName() : null);
        }
        List<GradingTaskResponse> records = pageResult.getRecords().stream()
                .map(t -> toResponse(t, fileNameMap.get(t.getFileId()))).collect(Collectors.toList());

        PageResult<GradingTaskResponse> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(pageResult.getTotal());
        result.setCurrent(pageResult.getCurrent());
        result.setSize(pageResult.getSize());
        return result;
    }

    @Override
    public PageResult<GradingTaskResponse> pageFileTasks(Long userId, String semester, String taskCategory,
                                                          Integer taskSequence, String studentId, Integer status,
                                                          Integer page, Integer size, String sortOrder) {
        LambdaQueryWrapper<GradingTask> wrapper = Wrappers.<GradingTask>lambdaQuery()
                .eq(GradingTask::getUserId, userId)
                .eq(GradingTask::getFileType, FileType.WORD)
                .eq(semester != null && !semester.isEmpty(), GradingTask::getSemester, semester)
                .eq(taskCategory != null && !taskCategory.isEmpty(), GradingTask::getTaskCategory, TaskCategory.fromCode(taskCategory))
                .eq(taskSequence != null, GradingTask::getTaskSequence, taskSequence)
                .eq(studentId != null && !studentId.isEmpty(), GradingTask::getStudentId, studentId)
                .eq(status != null, GradingTask::getStatus, GradingTaskStatus.getValue(status));

        boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
        wrapper.orderBy(true, isAsc, GradingTask::getCreateTime);

        Page<GradingTask> pageResult = baseMapper.selectPage(new Page<>(page, size), wrapper);
        List<String> fileIds = pageResult.getRecords().stream()
                .map(GradingTask::getFileId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<String, String> fileNameMap = new HashMap<>();
        for (String id : fileIds) {
            UploadFileDTO f = uploadService.selectById(id);
            fileNameMap.put(id, f != null ? f.getFileName() : null);
        }
        List<GradingTaskResponse> records = pageResult.getRecords().stream()
                .map(t -> toResponse(t, fileNameMap.get(t.getFileId()))).collect(Collectors.toList());

        PageResult<GradingTaskResponse> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(pageResult.getTotal());
        result.setCurrent(pageResult.getCurrent());
        result.setSize(pageResult.getSize());
        return result;
    }

    @Override
    public GradingTaskResponse getDetail(String taskId) {
        GradingTask task = baseMapper.selectById(taskId);
        if (task == null || task.getDeleted() == 1) {
            throw new BusinessException(ErrorCodeConstant.NOT_FOUND, ErrorMessageConstant.TASK_NOT_FOUND);
        }
        String fileName = null;
        if (task.getFileId() != null) {
            UploadFileDTO f = uploadService.selectById(task.getFileId());
            fileName = f != null ? f.getFileName() : null;
        }
        return toResponse(task, fileName);
    }

    private GradingTaskResponse toResponse(GradingTask task, String fileName) {
        return GradingTaskResponse.builder()
                .id(task.getId())
                .fileId(task.getFileId())
                .userId(task.getUserId())
                .rubricId(task.getRubricId())
                .studentCount(task.getStudentCount())
                .processedStudentCount(task.getProcessedStudentCount())
                .status(task.getStatus())
                .retryCount(task.getRetryCount())
                .errorMessage(task.getErrorMessage())
                .createTime(task.getCreateTime())
                .finishTime(task.getFinishTime())
                .taskCategory(task.getTaskCategory())
                .taskSequence(task.getTaskSequence())
                .classId(task.getClassId())
                .semester(task.getSemester())
                .studentId(task.getStudentId())
                .fileType(task.getFileType())
                .fileName(fileName)
                .build();
    }
}