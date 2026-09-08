package com.amlve.agentprepare.client;

import com.amlve.agentprepareapi.PrepareAgentClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 备课智能体客户端实现。
 * 当前版本返回 Mock 教案模板，Dify 服务就绪后替换为 WebClient 调用 Dify 工作流。
 */
@Component
@Slf4j
public class PrepareClient implements PrepareAgentClient {

    /**
     * 调用 Dify 获取教案文本（当前为 Mock 实现）
     *
     * @param lessonPrompt 备课提示词（含章节信息）
     * @return 教案内容
     */
    @Override
    public String getLessonText(String lessonPrompt) {
        log.info("调用备课智能体生成教案, lessonPrompt={}", lessonPrompt);
        // TODO Dify就绪后替换为真实调用：WebClient请求Dify工作流API，入参lessonPrompt
        return "【教案模板 - " + lessonPrompt + "】\n"
                + "一、教学目标：掌握本章核心知识点，能独立完成课后例题。\n"
                + "二、教学重难点：本章重点概念辨析与常见易错点。\n"
                + "三、课堂例题：结合教材例题讲解，演示完整解题步骤。\n"
                + "四、上机实训任务：安排教材配套实操案例，学生独立完成并提交。\n"
                + "五、课后作业：布置教材课后习题，巩固本章知识点。";
    }
}
