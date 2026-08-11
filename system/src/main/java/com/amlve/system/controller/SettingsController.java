package com.amlve.system.controller;

import com.amlve.common.request.Result;
import com.amlve.common.utils.UserContext;
import com.amlve.system.dto.StandardConfigRequest;
import com.amlve.system.service.SettingsService;
import com.amlve.system.service.StandardConfigService;
import com.amlve.system.vo.SettingsResponse;
import com.amlve.system.vo.StandardConfigResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final StandardConfigService standardConfigService;

    @GetMapping("/get")
    public Result<SettingsResponse> getSettings() {
        return Result.success(settingsService.getSettings(UserContext.getUserId()));
    }

    @PostMapping("/createStandard")
    public Result<StandardConfigResponse> createStandardSettings(@RequestBody StandardConfigRequest standardConfigRequest) {
        return Result.success(standardConfigService.createStandardSettings(UserContext.getUserId(),standardConfigRequest));
    }
}
