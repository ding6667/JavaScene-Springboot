package com.amlve.agentprepare.client;

import com.amlve.agentprepareapi.dto.request.PrepareRequest;
import com.amlve.agentprepareapi.dto.response.PrepareResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 备课智能体客户端实现。
 * 当前版本按请求开关返回 Mock 备课材料，Dify 服务就绪后替换为 WebClient 调用 Dify 工作流。
 */
@Component
@Slf4j
public class PrepareAgentClient implements com.amlve.agentprepareapi.PrepareAgentClient {

    /**
     * 生成备课材料（当前为 Mock 实现）
     *
     * @param request 备课请求（含章节ID、五个生成开关、用户额外指令）
     * @return 备课结果（含教案、PPT、重难点、作业、实训）
     */
    @Override
    public PrepareResponse generate(PrepareRequest request) {
        log.info("调用备课智能体生成备课材料, chapterId={}, pptx={}, lessonPlan={}, homework={}, lab={}, keyPoints={}",
                request.getChapterId(), request.getPptx(), request.getLessonPlan(),
                request.getHomework(), request.getLab(), request.getKeyPoints());
        // TODO Dify就绪后替换为真实调用：WebClient请求Dify工作流API，按开关编排工作流入参
        String chapter = "第" + request.getChapterId() + "章";

        PrepareResponse response = new PrepareResponse();
        if (isOn(request.getLessonPlan())) {
            response.setLessonPlan("【教案模板 - " + chapter + "】\n"
                    + "一、教学目标：掌握本章核心知识点，能独立完成课后例题。\n"
                    + "二、教学重难点：本章重点概念辨析与常见易错点。\n"
                    + "三、课堂例题：结合教材例题讲解，演示完整解题步骤。\n"
                    + "四、上机实训任务：安排教材配套实操案例，学生独立完成并提交。\n"
                    + "五、课后作业：布置教材课后习题，巩固本章知识点。");
        }
        if (isOn(request.getKeyPoints())) {
            response.setKeyPoints("【重难点清单 - " + chapter + "】\n"
                    + "1. 重点：本章核心概念及其应用场景。\n"
                    + "2. 难点：易混淆概念辨析与综合运用。");
        }
        if (isOn(request.getHomework())) {
            response.setHomework("【课后习题 - " + chapter + "】\n"
                    + "1. 简答题：概述本章核心概念及适用场景。\n"
                    + "2. 编程题：完成教材本章课后编程练习。");
        }
        if (isOn(request.getLab())) {
            response.setLab("【上机实训方案 - " + chapter + "】\n"
                    + "任务：基于本章知识点完成综合实操案例，并提交运行结果。");
        }
        if (isOn(request.getPptx())) {
            // Mock 占位地址，仅标识 PPT 已生成，接入 Dify 后替换为真实文件地址
            response.setPptxUrl("mock://ppt/" + request.getChapterId() + ".pptx");
        }
        return response;
    }

    /** 判断生成开关是否开启（"1"=开启，其余视为关闭） */
    private boolean isOn(String flag) {
        return "1".equals(flag);
    }
}
