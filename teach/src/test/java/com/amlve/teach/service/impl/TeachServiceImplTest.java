package com.amlve.teach.service.impl;

import com.amlve.common.security.UserIdAuthenticationToken;
import com.amlve.teach.dto.request.HomeworkPublishRequest;
import com.amlve.teach.dto.response.HomeworkResponse;
import com.amlve.teach.mapper.HomeworkMapper;
import com.amlve.teach.tableEnt.Homework;
import com.amlve.teachapi.dto.HomeworkDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TeachServiceImpl 单元测试。
 * 通过向 SecurityContextHolder 注入 UserIdAuthenticationToken 模拟登录教师，
 * 不依赖 JWT 过滤器，也无需修改任何公共模块代码。
 */
@ExtendWith(MockitoExtension.class)
class TeachServiceImplTest {

    @Mock
    private HomeworkMapper homeworkMapper;

    @InjectMocks
    private TeachServiceImpl teachService;

    private static final Long TEACHER_ID = 1L;

    @BeforeEach
    void setUpLoginTeacher() {
        // 模拟当前登录用户：userId=1 的教师（与运行时 JwtAuthenticationFilter 注入的结构一致）
        SecurityContextHolderTestUtil.loginAs(TEACHER_ID, "teacher");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolderTestUtil.logout();
    }

    private HomeworkPublishRequest buildRequest() {
        HomeworkPublishRequest request = new HomeworkPublishRequest();
        request.setTitle("第一次Java作业");
        request.setContent("完成教材第1章课后习题");
        request.setChapterId(1L);
        request.setDeadline(LocalDateTime.of(2026, 9, 15, 23, 59, 59));
        return request;
    }

    @Test
    void publishHomework_shouldMapFieldsAndInjectTeacherId() {
        // 模拟 MyBatis-Plus insert 主键回填
        doAnswer(invocation -> {
            Homework h = invocation.getArgument(0);
            h.setId(100L);
            return 1;
        }).when(homeworkMapper).insert(any(Homework.class));

        Long homeworkId = teachService.publishHomework(buildRequest());

        assertThat(homeworkId).isEqualTo(100L);
        // 验证落库实体字段映射完整，teacherId 从登录上下文注入
        ArgumentCaptor<Homework> captor = ArgumentCaptor.forClass(Homework.class);
        verify(homeworkMapper).insert(captor.capture());
        Homework saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("第一次Java作业");
        assertThat(saved.getContent()).isEqualTo("完成教材第1章课后习题");
        assertThat(saved.getChapterId()).isEqualTo(1L);
        assertThat(saved.getDeadline()).isEqualTo(LocalDateTime.of(2026, 9, 15, 23, 59, 59));
        assertThat(saved.getTeacherId()).isEqualTo(TEACHER_ID);
        assertThat(saved.getCreateTime()).isNotNull();
    }

    @Test
    void listMyHomework_shouldConvertEntityToResponse() {
        Homework homework = new Homework();
        homework.setId(100L);
        homework.setTitle("第一次Java作业");
        homework.setContent("完成教材第1章课后习题");
        homework.setChapterId(1L);
        homework.setTeacherId(TEACHER_ID);
        homework.setCreateTime(LocalDateTime.of(2026, 9, 7, 10, 0));
        when(homeworkMapper.selectList(any())).thenReturn(List.of(homework));

        List<HomeworkResponse> result = teachService.listMyHomework();

        assertThat(result).hasSize(1);
        HomeworkResponse response = result.get(0);
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getTitle()).isEqualTo("第一次Java作业");
        assertThat(response.getChapterId()).isEqualTo(1L);
    }

    @Test
    void getHomeworkById_shouldReturnDto_whenExists() {
        Homework homework = new Homework();
        homework.setId(100L);
        homework.setTitle("第一次Java作业");
        homework.setTeacherId(TEACHER_ID);
        when(homeworkMapper.selectById(100L)).thenReturn(homework);

        HomeworkDTO dto = teachService.getHomeworkById(100L);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getTitle()).isEqualTo("第一次Java作业");
    }

    @Test
    void getHomeworkById_shouldReturnNull_whenNotExists() {
        when(homeworkMapper.selectById(999L)).thenReturn(null);

        assertThat(teachService.getHomeworkById(999L)).isNull();
    }

    @Test
    void listHomeworkByTeacherId_shouldReturnDtoList() {
        Homework homework = new Homework();
        homework.setId(100L);
        homework.setTitle("第一次Java作业");
        homework.setTeacherId(TEACHER_ID);
        when(homeworkMapper.selectList(any())).thenReturn(List.of(homework));

        List<HomeworkDTO> dtoList = teachService.listHomeworkByTeacherId(TEACHER_ID);

        assertThat(dtoList).hasSize(1);
        assertThat(dtoList.get(0).getId()).isEqualTo(100L);
    }
}
