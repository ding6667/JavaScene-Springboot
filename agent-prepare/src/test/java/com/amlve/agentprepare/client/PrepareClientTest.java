package com.amlve.agentprepare.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 备课智能体客户端（Mock 版）单元测试。
 * Dify 就绪替换为真实调用后，此测试需同步改为校验 WebClient 行为。
 */
class PrepareClientTest {

    private final PrepareClient prepareClient = new PrepareClient();

    @Test
    void getLessonText_shouldContainLessonPrompt() {
        String plan = prepareClient.getLessonText("第3章");

        assertThat(plan).isNotBlank();
        assertThat(plan).contains("第3章");
    }

    @Test
    void getLessonText_shouldContainTeachingPlanStructure() {
        String plan = prepareClient.getLessonText("第1章");

        // 教案五要素：目标、重难点、例题、实训、作业
        assertThat(plan).contains("教学目标");
        assertThat(plan).contains("重难点");
        assertThat(plan).contains("例题");
        assertThat(plan).contains("实训");
        assertThat(plan).contains("课后作业");
    }

    @Test
    void getLessonText_shouldDistinguishPrompts() {
        String plan1 = prepareClient.getLessonText("第1章");
        String plan2 = prepareClient.getLessonText("第2章");

        assertThat(plan1).isNotEqualTo(plan2);
    }
}
