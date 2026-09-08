package com.amlve.agentprepare.client;

import com.amlve.agentprepareapi.dto.request.PrepareRequest;
import com.amlve.agentprepareapi.dto.response.PrepareResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 备课智能体客户端（Mock 版）单元测试。
 * Dify 就绪替换为真实调用后，此测试需同步改为校验 WebClient 行为。
 */
class PrepareAgentClientTest {

    private final PrepareAgentClient client = new PrepareAgentClient();

    @Test
    void generate_shouldReturnAllSections_whenAllSwitchesOn() {
        PrepareRequest request = new PrepareRequest();
        request.setChapterId(3L);

        PrepareResponse response = client.generate(request);

        // 开关全开时，五类材料均应生成
        assertThat(response.getLessonPlan()).contains("第3章");
        assertThat(response.getKeyPoints()).isNotBlank();
        assertThat(response.getHomework()).isNotBlank();
        assertThat(response.getLab()).isNotBlank();
        assertThat(response.getPptxUrl()).isNotBlank();
    }

    @Test
    void generate_shouldContainTeachingPlanStructure() {
        PrepareRequest request = new PrepareRequest();
        request.setChapterId(1L);

        String plan = client.generate(request).getLessonPlan();

        // 教案五要素：目标、重难点、例题、实训、作业
        assertThat(plan).contains("教学目标");
        assertThat(plan).contains("重难点");
        assertThat(plan).contains("例题");
        assertThat(plan).contains("实训");
        assertThat(plan).contains("课后作业");
    }

    @Test
    void generate_shouldSkipSections_whenSwitchesOff() {
        PrepareRequest request = new PrepareRequest();
        request.setChapterId(2L);
        request.setLessonPlan("0");
        request.setKeyPoints("0");
        request.setHomework("0");
        request.setLab("0");
        request.setPptx("0");

        PrepareResponse response = client.generate(request);

        // 开关全关时，各部分均不生成
        assertThat(response.getLessonPlan()).isNull();
        assertThat(response.getKeyPoints()).isNull();
        assertThat(response.getHomework()).isNull();
        assertThat(response.getLab()).isNull();
        assertThat(response.getPptxUrl()).isNull();
    }

    @Test
    void generate_shouldDistinguishChapters() {
        PrepareRequest req1 = new PrepareRequest();
        req1.setChapterId(1L);
        PrepareRequest req2 = new PrepareRequest();
        req2.setChapterId(2L);

        String plan1 = client.generate(req1).getLessonPlan();
        String plan2 = client.generate(req2).getLessonPlan();

        assertThat(plan1).isNotEqualTo(plan2);
    }
}
