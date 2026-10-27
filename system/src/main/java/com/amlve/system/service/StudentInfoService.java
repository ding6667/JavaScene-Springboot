package com.amlve.system.service;

import com.amlve.common.request.PageResult;
import com.amlve.system.dto.request.StudentInfoUpdateRequest;
import com.amlve.system.dto.request.StudentQueryRequest;
import com.amlve.system.dto.response.BatchImportResponse;
import com.amlve.system.dto.response.StudentInfoResponse;
import com.amlve.system.dto.response.StudentListItemResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 学生学籍扩展信息
 */
public interface StudentInfoService {

    /**
     * 查某学生的扩展信息，缺记录时返回只带 userId 的空对象
     */
    StudentInfoResponse getByUserId(Long userId);

    /**
     * 本人完善学生扩展信息：首次必须填学号，之后学号与班级只读
     */
    void updateSelf(Long userId, StudentInfoUpdateRequest request);

    /**
     * 新建用户时补一条空扩展记录，保证 /api/user/info 的 extInfo 非空
     */
    void createEmpty(Long userId);

    /**
     * 按班级范围分页查学生
     *
     * @param classIds  null 表示不限班级（管理员），空集合表示教师无可用班级
     */
    PageResult<StudentListItemResponse> pageStudents(List<Long> classIds, StudentQueryRequest request);

    /**
     * 批量导入学生。txt / md 文件，一行一名学生，字段以逗号分隔：学号,姓名[,初始密码]
     * <p>
     * 部分成功策略（定死，不再改）：逐行独立提交，单条失败不回滚整批，
     * 失败行连同行号与原因进 failList，成功行照常入库
     */
    BatchImportResponse batchAdd(Long classId, MultipartFile file);

    /**
     * 导入模板内容。格式定义与解析逻辑放在一起，避免改了 parser 忘了改模板
     */
    String importTemplate();

    /**
     * 导出学生名单为 CSV（含 UTF-8 BOM，Excel 直接双击不乱码）
     *
     * @param classIds null 表示不限班级，空集合表示无可见班级（导出空表头）
     */
    String exportCsv(List<Long> classIds, String keyword);

    /**
     * 某班全部学生ID，供跨模块组装与班级学情使用
     */
    List<Long> listUserIdsByClass(Long classId);

    /**
     * 教师作为任课教师所带的全部班级ID
     */
    List<Long> listClassIdsByTeacher(Long teacherId);

    /**
     * 学号是否已被占用
     */
    boolean existsStudentNo(String studentNo);
}
