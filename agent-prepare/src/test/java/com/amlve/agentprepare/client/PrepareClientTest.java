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
    void generateTeachingPlan_shouldContainChapterId() {
        String plan = prepareClient.generateTeachingPlan(3L);

        assertThat(plan).isNotBlank();
        assertThat(plan).contains("3");
    }

    @Test
    void generateTeachingPlan_shouldContainTeachingPlanStructure() {
        String plan = prepareClient.generateTeachingPlan(1L);

        // 教案五要素：目标、重难点、例题、实训、作业
        assertThat(plan).contains("教学目标");
        assertThat(plan).contains("重难点");
        assertThat(plan).contains("例题");
        assertThat(plan).contains("实训");
        assertThat(plan).contains("课后作业");
    }

    @Test
    void generateTeachingPlan_shouldDistinguishChapters() {
        String plan1 = prepareClient.generateTeachingPlan(1L);
        String plan2 = prepareClient.generateTeachingPlan(2L);

        assertThat(plan1).isNotEqualTo(plan2);
    }
}
