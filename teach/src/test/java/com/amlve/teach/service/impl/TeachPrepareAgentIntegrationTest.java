package com.amlve.teach.service.impl;

import com.amlve.agentprepare.bean.DifyClient;
import com.amlve.agentprepare.client.PrepareAgentClientImpl;
import com.amlve.common.exception.BusinessException;
import com.amlve.common.request.Result;
import com.amlve.knowledgeapi.dto.response.TeachingMaterialDTO;
import com.amlve.knowledgeapi.service.KnowledgeChapterApi;
import com.amlve.teach.dto.request.PrepareRequest;
import com.amlve.teach.dto.response.PrepareResponse;
import com.amlve.teach.mapper.TeachPptMapper;
import com.amlve.teach.mapper.TeachPrepareRecordMapper;
import com.amlve.teach.service.TeachPrepareService;
import com.amlve.teach.tableEnt.TeachPpt;
import com.amlve.teach.tableEnt.TeachPrepareRecord;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 备课智能体链路集成测试（真实落库）。
 * <p>
 * 场景：bootstrap 因其他模块 bean 冲突无法启动时，用精简上下文单独验证
 * teach 编排 + agent-prepare 真实实现 + 本地 MySQL 真实读写。
 * 数据源见 src/test/resources/application.yml（本地 javascene 库）。
 * 素材接口 Mock（实现归 knowledge 模块）；
 * agent-prepare 走真实 Dify（环境变量 DIFY_PREPARE_API_KEY 设置密钥时）或 Mock 兜底，
 * 断言对两种路径均成立（路径特有内容交给各层单测）。
 */
@SpringBootTest(classes = TeachPrepareAgentIntegrationTest.SlimApp.class)
class TeachPrepareAgentIntegrationTest {

    /** 集成测试专用章节ID，避免与真实数据冲突 */
    private static final Long CHAPTER_ID = 99001L;
    private static final Long TEACHER_ID = 1L;

    /**
     * 精简上下文：只装配 teach 模块 + agent-prepare 真实实现（不加载其他模块，规避 bean 冲突）
     */
    @SpringBootApplication(scanBasePackages = "com.amlve.teach")
    @Import({DifyClient.class, PrepareAgentClientImpl.class})
    static class SlimApp {

        /**
         * 测试专用时间填充：与 bootstrap 的 MyBatisPlusMetaHandler 行为一致
         * （运行时由 bootstrap 提供，精简上下文需自行注册，否则 fill 字段以 null 显式插入报错）
         */
        @org.springframework.context.annotation.Bean
        MetaObjectHandler testMetaObjectHandler() {
            return new MetaObjectHandler() {
                @Override
                public void insertFill(org.apache.ibatis.reflection.MetaObject metaObject) {
                    this.strictInsertFill(metaObject, "createTime", java.time.LocalDateTime.class, java.time.LocalDateTime.now());
                    this.strictInsertFill(metaObject, "updateTime", java.time.LocalDateTime.class, java.time.LocalDateTime.now());
                }

                @Override
                public void updateFill(org.apache.ibatis.reflection.MetaObject metaObject) {
                    this.strictUpdateFill(metaObject, "updateTime", java.time.LocalDateTime.class, java.time.LocalDateTime.now());
                }
            };
        }
    }

    @MockBean
    private KnowledgeChapterApi knowledgeChapterApi;

    @Resource
    private TeachPrepareService teachPrepareService;

    @Resource
    private TeachPrepareRecordMapper teachPrepareRecordMapper;

    @Resource
    private TeachPptMapper teachPptMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        SecurityContextHolderTestUtil.loginAs(TEACHER_ID, "teacher");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolderTestUtil.logout();
        // 物理清理测试数据，避免残留污染本地库
        jdbcTemplate.update("DELETE FROM teach_prepare_record WHERE chapter_id = ?", CHAPTER_ID);
        jdbcTemplate.update("DELETE FROM teach_ppt WHERE chapter_id = ?", CHAPTER_ID);
    }

    @Test
    void generateTeachingMaterials_success_persistsRecord() {
        // 素材接口返回模拟数据（实现归 knowledge 模块，不在本上下文内）
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID)).thenReturn(Result.success(mockMaterial()));

        PrepareRequest request = new PrepareRequest();
        request.setChapterId(CHAPTER_ID);
        request.setLessonPlan("1");
        request.setPptx("1");
        request.setHomework("1");
        request.setLab("1");
        request.setKeyPoints("1");
        request.setUserMessage("重点讲解变量与循环");

        PrepareResponse response = teachPrepareService.generateTeachingMaterials(request);

        // 响应断言：路径无关（真实 Dify 返回 LLM 生成内容，Mock 兜底返回模板内容，均非空）
        assertThat(response.getRecordId()).isNotNull();
        assertThat(response.getLessonPlan()).isNotBlank();
        assertThat(response.getKeyPoints()).isNotBlank();
        assertThat(response.getHomework()).isNotBlank();
        assertThat(response.getLab()).isNotBlank();

        // 落库断言：teach_prepare_record 成功记录（status=1）
        TeachPrepareRecord record = teachPrepareRecordMapper.selectById(response.getRecordId());
        assertThat(record).isNotNull();
        assertThat(record.getStatus()).isEqualTo(1);
        assertThat(record.getTeacherId()).isEqualTo(TEACHER_ID);
        assertThat(record.getChapterId()).isEqualTo(CHAPTER_ID);
        assertThat(record.getLessonPlan()).isNotBlank();
        assertThat(record.getPptxMd()).isNotBlank();
        assertThat(record.getErrorMsg()).isNull();

        // PPT 文件记录：本机未装 marp2pptx 时降级（pptxUrl=null 不落 teach_ppt）；已装则应有一行文件记录
        Long pptCount = teachPptMapper.selectCount(
                new LambdaQueryWrapper<TeachPpt>()
                        .eq(TeachPpt::getChapterId, CHAPTER_ID));
        if (response.getPptxUrl() != null) {
            assertThat(pptCount).isEqualTo(1);
        } else {
            assertThat(record.getPptxUrl()).isNull();
            assertThat(pptCount).isEqualTo(0);
        }
    }

    @Test
    void generateTeachingMaterials_failure_savesFailedRecord() {
        // 素材查询异常 → 失败记录落库（status=0）后业务异常上抛
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID))
                .thenThrow(new RuntimeException("knowledge 素材服务不可用"));

        PrepareRequest request = new PrepareRequest();
        request.setChapterId(CHAPTER_ID);
        request.setLessonPlan("1");

        assertThatThrownBy(() -> teachPrepareService.generateTeachingMaterials(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("备课生成失败");

        List<TeachPrepareRecord> failed = teachPrepareRecordMapper.selectList(
                new LambdaQueryWrapper<TeachPrepareRecord>()
                        .eq(TeachPrepareRecord::getChapterId, CHAPTER_ID)
                        .eq(TeachPrepareRecord::getStatus, 0));
        assertThat(failed).hasSize(1);
        assertThat(failed.get(0).getTeacherId()).isEqualTo(TEACHER_ID);
        assertThat(failed.get(0).getErrorMsg()).contains("素材服务不可用");
    }

    /** 模拟 knowledge 返回的章节素材（七字段，与真实结构一致） */
    private TeachingMaterialDTO mockMaterial() {
        return TeachingMaterialDTO.builder()
                .chapterSummary("第1章 Java基础：变量、数据类型、流程控制")
                .conceptDefinitions("[{\"name\":\"变量\",\"definition\":\"内存中的命名存储位置\"}]")
                .codeExamples("[{\"title\":\"HelloWorld\",\"code\":\"System.out.println(1);\"}]")
                .commonMistakes("[{\"mistake\":\"未初始化局部变量\",\"tip\":\"使用前必须赋值\"}]")
                .pageReferences("[{\"page\":12,\"content\":\"变量声明语法\"}]")
                .exercises("[{\"title\":\"选择题1\",\"answer\":\"B\"}]")
                .labTasks("[]")
                .build();
    }
}
