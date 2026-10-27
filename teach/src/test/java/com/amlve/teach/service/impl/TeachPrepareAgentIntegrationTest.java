package com.amlve.teach.service.impl;

import com.amlve.agentprepare.bean.DifyClient;
import com.amlve.agentprepare.client.PrepareAgentClientImpl;
import com.amlve.common.request.Result;
import com.amlve.common.utils.FileFormatUtil;
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
import org.springframework.scheduling.annotation.EnableAsync;

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
 * <p>
 * 备课已改为异步：受理只校验并落 status=2 记录，材料由后台线程回写，
 * 因此断言需要先等记录走到终态（等价于前端轮询 detail）。
 */
@SpringBootTest(classes = TeachPrepareAgentIntegrationTest.SlimApp.class)
class TeachPrepareAgentIntegrationTest {

    /** 集成测试专用章节ID，避免与真实数据冲突 */
    private static final Long CHAPTER_ID = 99001L;
    private static final Long TEACHER_ID = 1L;

    /** 后台生成终态等待上限（真实 Dify 读取超时 120s + PPT 转换） */
    private static final java.time.Duration GENERATION_TIMEOUT = java.time.Duration.ofMinutes(4);

    /**
     * 精简上下文：只装配 teach 模块 + agent-prepare 真实实现（不加载其他模块，规避 bean 冲突）。
     * {@code @EnableAsync} 与 bootstrap 的 WebApplication 保持一致，否则 generateAsync 退化为同步执行。
     */
    @SpringBootApplication(scanBasePackages = "com.amlve.teach")
    @EnableAsync
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

    /** 发布闭环依赖的题库接口：集成上下文无 exam 模块实现，Mock 满足 Bean 装配 */
    @MockBean
    private com.amlve.examapi.service.ExamQuestionApi examQuestionApi;

    /**
     * Word 导出工具（com.amlve.common 包，不在 SlimApp 扫描范围）。
     * 集成测试只覆盖备课生成链路，导出能力 Mock 掉即可满足 Bean 装配。
     */
    @MockBean
    private FileFormatUtil fileFormatUtil;

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
    void generateTeachingMaterials_acceptsThenPersistsRecordAsync() {
        // 素材接口返回模拟数据（实现归 knowledge 模块，不在本上下文内）
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID)).thenReturn(Result.success(mockMaterial()));

        PrepareResponse response = teachPrepareService.generateTeachingMaterials(buildRequest());

        // 受理响应：秒回记录ID + 生成中，不含五类材料
        assertThat(response.getRecordId()).isNotNull();
        assertThat(response.getChapterId()).isEqualTo(CHAPTER_ID);
        assertThat(response.getStatus()).isEqualTo(2);
        assertThat(response.getStatusDesc()).isEqualTo("生成中");

        // 等后台线程把材料回写、状态推进到终态（前端轮询 detail 的同一步骤）
        TeachPrepareRecord record = awaitTerminalRecord(response.getRecordId());
        assertThat(record.getStatus()).isEqualTo(1);
        assertThat(record.getTeacherId()).isEqualTo(TEACHER_ID);
        assertThat(record.getChapterId()).isEqualTo(CHAPTER_ID);
        assertThat(record.getLessonPlan()).isNotBlank();
        assertThat(record.getKeyPoints()).isNotBlank();
        assertThat(record.getHomework()).isNotBlank();
        assertThat(record.getLab()).isNotBlank();
        assertThat(record.getPptxMd()).isNotBlank();
        assertThat(record.getErrorMsg()).isNull();

        // PPT 文件记录：本机未装 marp2pptx 时降级（pptxUrl=null 不落 teach_ppt）；已装则应有一行文件记录
        Long pptCount = teachPptMapper.selectCount(
                new LambdaQueryWrapper<TeachPpt>()
                        .eq(TeachPpt::getChapterId, CHAPTER_ID));
        if (record.getPptxUrl() != null) {
            assertThat(pptCount).isEqualTo(1);
        } else {
            assertThat(pptCount).isEqualTo(0);
        }
    }

    @Test
    void generateTeachingMaterials_shouldNotPersistAnyRecord_whenValidationFails() {
        // 素材查询异常：校验发生在落库之前，直接上抛且不留下半成品记录（失败终态由后台生成阶段负责）
        when(knowledgeChapterApi.getTeachingMaterials(CHAPTER_ID))
                .thenThrow(new RuntimeException("knowledge 素材服务不可用"));

        PrepareRequest request = new PrepareRequest();
        request.setChapterId(CHAPTER_ID);
        request.setLessonPlan("1");

        assertThatThrownBy(() -> teachPrepareService.generateTeachingMaterials(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("knowledge 素材服务不可用");

        List<TeachPrepareRecord> records = teachPrepareRecordMapper.selectList(
                new LambdaQueryWrapper<TeachPrepareRecord>()
                        .eq(TeachPrepareRecord::getChapterId, CHAPTER_ID));
        assertThat(records).isEmpty();
    }

    /** 轮询等待备课记录离开"生成中"（status=2），等价于前端轮询 detail */
    private TeachPrepareRecord awaitTerminalRecord(Long recordId) {
        long deadline = System.currentTimeMillis() + GENERATION_TIMEOUT.toMillis();
        while (System.currentTimeMillis() < deadline) {
            TeachPrepareRecord record = teachPrepareRecordMapper.selectById(recordId);
            if (record != null && !Integer.valueOf(2).equals(record.getStatus())) {
                return record;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new AssertionError("备课后台生成超时，记录仍停在生成中: recordId=" + recordId);
    }

    /** 组装备课请求（五个生成开关全开） */
    private PrepareRequest buildRequest() {
        PrepareRequest request = new PrepareRequest();
        request.setChapterId(CHAPTER_ID);
        request.setLessonPlan("1");
        request.setPptx("1");
        request.setHomework("1");
        request.setLab("1");
        request.setKeyPoints("1");
        request.setUserMessage("重点讲解变量与循环");
        return request;
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
