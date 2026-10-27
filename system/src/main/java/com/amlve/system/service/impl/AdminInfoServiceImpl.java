package com.amlve.system.service.impl;

import com.amlve.system.constant.ErrorMessageConstant;
import com.amlve.common.constant.ErrorCodeConstant;
import com.amlve.common.exception.BusinessException;
import com.amlve.system.mapper.SysAdminInfoMapper;
import com.amlve.system.service.AdminInfoService;
import com.amlve.system.dto.response.AdminInfoResponse;
import com.amlve.system.tableEnt.SysAdminInfo;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminInfoServiceImpl implements AdminInfoService {

    private final SysAdminInfoMapper adminInfoMapper;

    @Override
    public AdminInfoResponse getByUserId(Long userId) {
        SysAdminInfo info = adminInfoMapper.selectOne(
                Wrappers.<SysAdminInfo>lambdaQuery().eq(SysAdminInfo::getUserId, userId));
        AdminInfoResponse vo = new AdminInfoResponse();
        vo.setUserId(userId);
        if (info != null) {
            vo.setAdminNo(info.getAdminNo());
            vo.setPermissionLevel(info.getPermissionLevel());
            vo.setDepartment(info.getDepartment());
        }
        return vo;
    }

    @Override
    public void createEmpty(Long userId) {
        if (adminInfoMapper.selectCount(Wrappers.<SysAdminInfo>lambdaQuery()
                .eq(SysAdminInfo::getUserId, userId)) > 0) {
            throw new BusinessException(ErrorCodeConstant.PARAM_INVALID, ErrorMessageConstant.EXT_INFO_EXISTS);
        }
        SysAdminInfo info = new SysAdminInfo();
        info.setUserId(userId);
        info.setPermissionLevel(1);
        adminInfoMapper.insert(info);
    }
}
