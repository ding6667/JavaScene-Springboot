package com.amlve.exam.service.impl;

import com.amlve.common.security.UserIdAuthenticationToken;
import com.amlve.exam.dto.response.DifficultyConfigVO;
import com.amlve.exam.mapper.ExamQuestionDifficultyConfigMapper;
import com.amlve.exam.tableEnt.ExamQuestionDifficultyConfig;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 难度配置业务实现单元测试：可用列表映射、批量取名与按编码回落。
 */
@ExtendWith(MockitoExtension.class)
class ExamDifficultyConfigServiceImplTest {

    @Mock
    private ExamQuestionDifficultyConfigMapper difficultyConfigMapper;

    @InjectMocks
    private ExamDifficultyConfigServiceImpl service;

    @BeforeEach
    void login() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UserIdAuthenticationToken(1L, "teacher01", null));
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    private ExamQuestionDifficultyConfig config(Long id, String code, String name) {
        ExamQuestionDifficultyConfig config = new ExamQuestionDifficultyConfig();
        config.setId(id);
        config.setCode(code);
        config.setName(name);
        config.setLevelOrder(id.intValue());
        config.setIsSystem(1);
        return config;
    }

    @Test
    void listUsable_shouldMapFieldsForFrontend() {
        when(difficultyConfigMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(config(1L, "basic", "简单"), config(3L, "advanced", "拔高")));

        List<DifficultyConfigVO> result = service.listUsable(5L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getCode()).isEqualTo("basic");
        assertThat(result.get(0).getName()).isEqualTo("简单");
        assertThat(result.get(1).getName()).isEqualTo("拔高");
    }

    @Test
    void namesByIds_shouldBuildIdNameMap_andIgnoreNullIds() {
        when(difficultyConfigMapper.selectBatchIds(any()))
                .thenReturn(List.of(config(1L, "basic", "简单"), config(2L, "intermediate", "进阶")));

        Map<Long, String> names = service.namesByIds(java.util.Arrays.asList(1L, 2L, null, 1L));

        assertThat(names).containsEntry(1L, "简单").containsEntry(2L, "进阶").hasSize(2);
    }

    @Test
    void namesByIds_shouldReturnEmptyMap_andSkipQuery_whenNoValidIds() {
        assertThat(service.namesByIds(List.of())).isEmpty();
        assertThat(service.namesByIds(java.util.Arrays.asList((Long) null))).isEmpty();
        verify(difficultyConfigMapper, never()).selectBatchIds(any());
    }

    @Test
    void nameOf_shouldReturnNull_whenIdNullOrConfigMissing() {
        assertThat(service.nameOf(null)).isNull();
        when(difficultyConfigMapper.selectById(9L)).thenReturn(null);
        assertThat(service.nameOf(9L)).isNull();
    }

    @Test
    void idByCode_shouldReturnFirstMatch_andNullWhenAbsentOrBlank() {
        when(difficultyConfigMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(config(2L, "intermediate", "进阶")));

        assertThat(service.idByCode("intermediate")).isEqualTo(2L);
        assertThat(service.idByCode("  ")).isNull();
        assertThat(service.idByCode(null)).isNull();
    }

    @Test
    void idByCode_shouldReturnNull_whenNoConfigMatches() {
        when(difficultyConfigMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertThat(service.idByCode("custom_not_exist")).isNull();
    }
}
