package com.amlve.system.controller;

import com.amlve.common.request.Result;
import com.amlve.common.utils.UserContext;
import com.amlve.system.dto.StandardConfigDTO;
import com.amlve.system.service.SettingsService;
import com.amlve.system.service.StandardConfigService;
import com.amlve.system.vo.SettingsVO;
import com.amlve.system.vo.StandardConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final StandardConfigService standardConfigService;

    @GetMapping("/get")
    public Result<SettingsVO> getSettings() {
        return Result.success(settingsService.getSettings(UserContext.getUserId()));
    }

    @PostMapping("/createStandard")
    public Result<StandardConfigVO> createStandardSettings(@RequestBody StandardConfigDTO standardConfigDTO) {
        return Result.success(standardConfigService.createStandardSettings(UserContext.getUserId(),standardConfigDTO));
    }
}
