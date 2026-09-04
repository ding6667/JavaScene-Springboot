package com.amlve.system.controller;

import com.amlve.common.request.PageResult;
import com.amlve.common.request.Result;
import com.amlve.system.dto.request.ClassRequest;
import com.amlve.system.dto.response.ClassResponse;
import com.amlve.system.service.ClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/class")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;

    @PostMapping("/create")
    public Result<ClassResponse> create(@RequestBody ClassRequest request) {
        return Result.success(classService.create(request));
    }

    @PutMapping("/update")
    public Result<ClassResponse> update(@RequestBody ClassRequest request) {
        return Result.success(classService.update(request));
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        classService.delete(id);
        return Result.success(null);
    }

    @GetMapping("/get/{id}")
    public Result<ClassResponse> getById(@PathVariable Long id) {
        return Result.success(classService.getById(id));
    }

    @GetMapping("/records")
    public Result<PageResult<ClassResponse>> records(@RequestParam(defaultValue = "1") Long pageNum
            , @RequestParam(defaultValue = "10") Long pageSize
            , @RequestParam(required = false) String className
            , @RequestParam(defaultValue = "false") Boolean isAsc) {
        return Result.success(classService.records(pageNum, pageSize, className, isAsc));
    }
}