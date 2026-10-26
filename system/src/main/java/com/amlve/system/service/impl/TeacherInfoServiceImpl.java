package com.amlve.system.service.impl;

import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.system.dto.request.TeacherInfoUpdateRequest;
import com.amlve.system.dto.response.TeacherInfoResponse;
import com.amlve.system.mapper.SysTeacherInfoMapper;
import com.amlve.system.service.TeacherInfoService;
import com.amlve.system.tableEnt.SysTeacherInfo;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TeacherInfoServiceImpl implements TeacherInfoService {

    private final SysTeacherInfoMapper teacherInfoMapper;

    @Override
    public TeacherInfoResponse getByUserId(Long userId) {
        SysTeacherInfo info = selectByUserId(userId);
        TeacherInfoResponse vo = new TeacherInfoResponse();
        vo.setUserId(userId);
        if (info != null) {
            vo.setTeacherNo(info.getTeacherNo());
            vo.setTitle(info.getTitle());
            vo.setCollege(info.getCollege());
            vo.setDepartment(info.getDepartment());
            vo.setOffice(info.getOffice());
            vo.setResearchArea(info.getResearchArea());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSelf(Long userId, TeacherInfoUpdateRequest request) {
        SysTeacherInfo exist = selectByUserId(userId);
        boolean firstFill = exist == null || !StringUtils.hasText(exist.getTeacherNo());

        if (firstFill) {
            if (!StringUtils.hasText(request.getTeacherNo())) {
                throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, "首次完善必须填写工号");
            }
            if (existsTeacherNo(request.getTeacherNo())) {
                throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, "工号已被占用");
            }
            SysTeacherInfo entity = exist == null ? new SysTeacherInfo() : exist;
            entity.setUserId(userId);
            entity.setTeacherNo(request.getTeacherNo());
            applyEditable(entity, request);
            saveOrUpdate(entity);
            return;
        }

        // 已完善过：工号忽略，只更新可改字段
        SysTeacherInfo update = new SysTeacherInfo();
        update.setId(exist.getId());
        applyEditable(update, request);
        teacherInfoMapper.updateById(update);
    }

    @Override
    public void createEmpty(Long userId) {
        if (teacherInfoMapper.selectCount(Wrappers.<SysTeacherInfo>lambdaQuery()
                .eq(SysTeacherInfo::getUserId, userId)) > 0) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, "该用户已存在教师扩展记录");
        }
        SysTeacherInfo info = new SysTeacherInfo();
        info.setUserId(userId);
        teacherInfoMapper.insert(info);
    }

    @Override
    public boolean existsTeacherNo(String teacherNo) {
        return teacherInfoMapper.selectCount(Wrappers.<SysTeacherInfo>lambdaQuery()
                .eq(SysTeacherInfo::getTeacherNo, teacherNo)) > 0;
    }

    private void applyEditable(SysTeacherInfo entity, TeacherInfoUpdateRequest request) {
        entity.setTitle(request.getTitle());
        entity.setCollege(request.getCollege());
        entity.setDepartment(request.getDepartment());
        entity.setOffice(request.getOffice());
        entity.setResearchArea(request.getResearchArea());
    }

    private SysTeacherInfo selectByUserId(Long userId) {
        return teacherInfoMapper.selectOne(Wrappers.<SysTeacherInfo>lambdaQuery()
                .eq(SysTeacherInfo::getUserId, userId));
    }

    private void saveOrUpdate(SysTeacherInfo entity) {
        if (entity.getId() == null) {
            teacherInfoMapper.insert(entity);
        } else {
            teacherInfoMapper.updateById(entity);
        }
    }
}
