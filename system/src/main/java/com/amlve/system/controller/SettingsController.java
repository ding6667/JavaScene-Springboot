//package com.amlve.system.controller;
//
//import com.amlve.common.request.Result;
//import com.amlve.common.utils.SecurityUtils;
//import com.amlve.system.dto.request.CommonSettingsRequest;
//import com.amlve.system.dto.request.StandardConfigRequest;
//import com.amlve.system.dto.request.StandardConfigUpdateRequest;
//import com.amlve.system.dto.response.SettingsResponse;
//import com.amlve.system.dto.response.StandardConfigResponse;
//import com.amlve.system.service.SettingsService;
//import com.amlve.system.service.StandardConfigService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/settings")
//@RequiredArgsConstructor
//public class SettingsController {
//
//    private final SettingsService settingsService;
//    private final StandardConfigService standardConfigService;
//
//    //=====================================commonSettings=====================================
//    @GetMapping("/getCommonSettings")
//    public Result<SettingsResponse> getCommonSettings() {
//        return Result.success(settingsService.getCommonSettings(SecurityUtils.getUserId()));
//    }
//
//    @PutMapping("/updateCommonSettings")
//    public Result<SettingsResponse> updateCommonSettings(@RequestBody CommonSettingsRequest settingsRequest) {
//        return Result.success(settingsService.updateCommonSettings(SecurityUtils.getUserId(),settingsRequest));
//    }
//
//    @PutMapping("/defaultSettings")
//    public Result<SettingsResponse> defaultSettings() {
//        return Result.success(settingsService.defaultSettings(SecurityUtils.getUserId()));
//    }
//
//    //=====================================userSettings=====================================
//
//
//
//    //=====================================StandardConfig=====================================
//
//    @PostMapping("/createStandard")
//    public Result<StandardConfigResponse> createStandardSettings(@RequestBody StandardConfigRequest standardConfigRequest) {
//        return Result.success(standardConfigService.createStandardSettings(SecurityUtils.getUserId(),standardConfigRequest));
//    }
//
//    @GetMapping("/getStandard")
//    public Result<List<StandardConfigResponse>> getStandard() {
//        return Result.success(standardConfigService.getByUserId(SecurityUtils.getUserId()));
//    }
//
//    @PutMapping("/updateStandard/{id}")
//    public Result<StandardConfigResponse> updateStandard(@PathVariable Long id,
//               @Valid @RequestBody StandardConfigUpdateRequest request) {
//        return Result.success(standardConfigService.updateStandard(id, request));
//    }
//
//    @DeleteMapping("/deleteStandard/{id}")
//    public Result<Void> deleteStandard(@PathVariable Long id) {
//        standardConfigService.deleteStandard(id);
//        return Result.success(null);
//    }
//
//}