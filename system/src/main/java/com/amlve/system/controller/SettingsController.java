package com.amlve.system.controller;

import com.amlve.common.request.Result;
import com.amlve.common.utils.UserContext;
import com.amlve.system.dto.StandardConfigRequest;
import com.amlve.system.dto.StandardConfigUpdateRequest;
import com.amlve.system.service.SettingsService;
import com.amlve.system.service.StandardConfigService;
import com.amlve.system.vo.SettingsResponse;
import com.amlve.system.vo.StandardConfigResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/getStandard")
    public Result<List<StandardConfigResponse>> getStandard() {
        return Result.success(standardConfigService.getByUserId(UserContext.getUserId()));
    }

    @PutMapping("/updateStandard/{id}")
    public Result<StandardConfigResponse> updateStandard(@PathVariable Long id,
               @Valid @RequestBody StandardConfigUpdateRequest request) {
        return Result.success(standardConfigService.updateStandard(id, request));
    }

    @DeleteMapping("/deleteStandard/{id}")
    public Result<Void> deleteStandard(@PathVariable Long id) {
        standardConfigService.deleteStandard(id);
        return Result.success(null);
    }

}