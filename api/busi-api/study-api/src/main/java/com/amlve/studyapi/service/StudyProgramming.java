package com.amlve.studyapi.service;

import com.amlve.common.request.Result;
import com.amlve.studyapi.dto.resquest.ProgrammingJudgeRequest;
import com.amlve.studyapi.dto.response.ProgrammingJudgeResponse;

public interface StudyProgramming {
    /**
     * 作业场景：exam 把编程题提交转给 study 判题
     * 【exam 调】作业批改时，编程题交给 study 跑用例判分
     */
    Result<ProgrammingJudgeResponse> judgeProgramming(ProgrammingJudgeRequest request);
}
