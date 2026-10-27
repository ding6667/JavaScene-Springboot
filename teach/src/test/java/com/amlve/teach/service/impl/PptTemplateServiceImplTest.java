package com.amlve.teach.service.impl;

import com.amlve.common.exception.BusinessException;
import com.amlve.teach.dto.request.PptTemplateSaveRequest;
import com.amlve.teach.dto.response.PptTemplateDetailVO;
import com.amlve.teach.dto.response.PptTemplateResponse;
import com.amlve.teach.dto.response.ThemeBriefResponse;
import com.amlve.teach.mapper.TeachPptTemplateMapper;
import com.amlve.teach.mapper.TeachPptThemeMapper;
import com.amlve.teach.tableEnt.TeachPptTemplate;
import com.amlve.teach.tableEnt.TeachPptTheme;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PPT 模板管理单元测试：列表映射、系统/自定义模板可见性、保存的增改分支与越权拒绝。
 */
@ExtendWith(MockitoExtension.class)
class PptTemplateServiceImplTest {

    @Mock
    private TeachPptTemplateMapper teachPptTemplateMapper;

    @Mock
    private TeachPptThemeMapper teachPptThemeMapper;

    @InjectMocks
    private PptTemplateServiceImpl service;

    private static final Long TEACHER_ID = 1L;
    private static final Long OTHER_TEACHER_ID = 99L;
    private static final Long TEMPLATE_ID = 10L;

    @BeforeEach
    void login() {
        SecurityContextHolderTestUtil.loginAs(TEACHER_ID, "teacher01");
    }

    @AfterEach
    void logout() {
        SecurityContextHolderTestUtil.logout();
    }

    /** 系统模板（所有教师可见） */
    private TeachPptTemplate systemTemplate() {
        TeachPptTemplate template = new TeachPptTemplate();
        template.setId(TEMPLATE_ID);
        template.setName("Java标准教学");
        template.setDescription("目录→知识点→代码→总结");
        template.setThemeCode("java-default");
        template.setStructureMd("# 第X章\n\n---\n\n# 总结");
        template.setIsSystem(1);
        template.setTeacherId(null);
        template.setIsActive(1);
        template.setSortOrder(1);
        return template;
    }

    /** 本人自定义模板 */
    private TeachPptTemplate ownTemplate() {
        TeachPptTemplate template = systemTemplate();
        template.setId(20L);
        template.setName("我的案例模板");
        template.setThemeCode("java-uncover");
        template.setIsSystem(0);
        template.setTeacherId(TEACHER_ID);
        template.setSortOrder(0);
        return template;
    }

    @Test
    void listTemplates_shouldMapItems() {
        when(teachPptTemplateMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(systemTemplate(), ownTemplate()));

        List<PptTemplateResponse> result = service.listTemplates();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Java标准教学");
        assertThat(result.get(0).getThemeCode()).isEqualTo("java-default");
        assertThat(result.get(0).getIsSystem()).isEqualTo(1);
        assertThat(result.get(1).getName()).isEqualTo("我的案例模板");
        assertThat(result.get(1).getIsSystem()).isEqualTo(0);
    }

    @Test
    void getTemplateDetail_shouldReturnStructure_whenSystemTemplate() {
        when(teachPptTemplateMapper.selectById(TEMPLATE_ID)).thenReturn(systemTemplate());

        PptTemplateDetailVO vo = service.getTemplateDetail(TEMPLATE_ID);

        assertThat(vo.getStructureMd()).contains("# 总结");
        assertThat(vo.getThemeCode()).isEqualTo("java-default");
        assertThat(vo.getIsSystem()).isEqualTo(1);
    }

    @Test
    void getTemplateDetail_shouldReturnStructure_whenOwnCustomTemplate() {
        when(teachPptTemplateMapper.selectById(20L)).thenReturn(ownTemplate());

        PptTemplateDetailVO vo = service.getTemplateDetail(20L);

        assertThat(vo.getStructureMd()).isNotBlank();
        assertThat(vo.getTeacherId()).isEqualTo(TEACHER_ID);
    }

    @Test
    void getTemplateDetail_shouldReject_whenCustomTemplateNotOwner() {
        TeachPptTemplate others = ownTemplate();
        others.setTeacherId(OTHER_TEACHER_ID);
        when(teachPptTemplateMapper.selectById(20L)).thenReturn(others);

        assertThatThrownBy(() -> service.getTemplateDetail(20L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("没有访问权限");
    }

    @Test
    void getTemplateDetail_shouldReject_whenNotFound() {
        when(teachPptTemplateMapper.selectById(any(Long.class))).thenReturn(null);

        assertThatThrownBy(() -> service.getTemplateDetail(TEMPLATE_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("没有找到这个模板");
    }

    @Test
    void saveTemplate_shouldInsertCustomTemplate_whenIdNull() {
        // insert 主键回填模拟
        doAnswer(invocation -> {
            TeachPptTemplate t = invocation.getArgument(0);
            t.setId(77L);
            return 1;
        }).when(teachPptTemplateMapper).insert(any(TeachPptTemplate.class));

        PptTemplateSaveRequest request = new PptTemplateSaveRequest();
        request.setName("新模板");
        request.setDescription("说明");
        request.setThemeCode("java-minimal");
        request.setStructureMd("# 骨架\n\n---\n\n# 总结");

        Long id = service.saveTemplate(request);

        assertThat(id).isEqualTo(77L);
        ArgumentCaptor<TeachPptTemplate> captor = ArgumentCaptor.forClass(TeachPptTemplate.class);
        verify(teachPptTemplateMapper).insert(captor.capture());
        TeachPptTemplate saved = captor.getValue();
        // 新增必须是自定义模板：归属当前教师、默认启用
        assertThat(saved.getIsSystem()).isZero();
        assertThat(saved.getTeacherId()).isEqualTo(TEACHER_ID);
        assertThat(saved.getIsActive()).isEqualTo(1);
        assertThat(saved.getName()).isEqualTo("新模板");
        assertThat(saved.getThemeCode()).isEqualTo("java-minimal");
    }

    @Test
    void saveTemplate_shouldUpdateFields_whenOwnTemplate() {
        when(teachPptTemplateMapper.selectById(20L)).thenReturn(ownTemplate());

        PptTemplateSaveRequest request = new PptTemplateSaveRequest();
        request.setId(20L);
        request.setName("改名后的模板");
        request.setThemeCode("java-dark");
        request.setStructureMd("# 新骨架");

        Long id = service.saveTemplate(request);

        assertThat(id).isEqualTo(20L);
        ArgumentCaptor<TeachPptTemplate> captor = ArgumentCaptor.forClass(TeachPptTemplate.class);
        verify(teachPptTemplateMapper).updateById(captor.capture());
        TeachPptTemplate updated = captor.getValue();
        assertThat(updated.getName()).isEqualTo("改名后的模板");
        assertThat(updated.getThemeCode()).isEqualTo("java-dark");
        assertThat(updated.getStructureMd()).isEqualTo("# 新骨架");
    }

    @Test
    void saveTemplate_shouldReject_whenSystemTemplate() {
        when(teachPptTemplateMapper.selectById(TEMPLATE_ID)).thenReturn(systemTemplate());

        PptTemplateSaveRequest request = new PptTemplateSaveRequest();
        request.setId(TEMPLATE_ID);
        request.setName("试图改系统模板");
        request.setThemeCode("java-default");
        request.setStructureMd("# x");

        assertThatThrownBy(() -> service.saveTemplate(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("系统内置模板不能修改");
    }

    @Test
    void saveTemplate_shouldReject_whenNotOwner() {
        TeachPptTemplate others = ownTemplate();
        others.setTeacherId(OTHER_TEACHER_ID);
        when(teachPptTemplateMapper.selectById(20L)).thenReturn(others);

        PptTemplateSaveRequest request = new PptTemplateSaveRequest();
        request.setId(20L);
        request.setName("试图改他人模板");
        request.setThemeCode("java-default");
        request.setStructureMd("# x");

        assertThatThrownBy(() -> service.saveTemplate(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("没有访问权限");
    }

    @Test
    void listThemes_shouldReturnFourThemes() {
        when(teachPptThemeMapper.selectList(any())).thenReturn(List.of(
                new TeachPptTheme().setThemeCode("java-default").setName("标准教学蓝").setVersion(1),
                new TeachPptTheme().setThemeCode("java-dark").setName("深色代码").setVersion(1),
                new TeachPptTheme().setThemeCode("java-uncover").setName("浅色简约").setVersion(1),
                new TeachPptTheme().setThemeCode("java-minimal").setName("极简白").setVersion(1)));

        List<ThemeBriefResponse> themes = service.listThemes();

        assertThat(themes).hasSize(4);
        assertThat(themes).extracting(ThemeBriefResponse::getThemeCode)
                .containsExactly("java-default", "java-dark", "java-uncover", "java-minimal");
        assertThat(themes).allMatch(t -> t.getName() != null && t.getVersion() != null);
    }
}
