package com.amlve.teach.mapper;

import com.amlve.teach.tableEnt.TeachPrepareRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 备课记录表数据库操作，MyBatis-Plus 提供通用增删改查
 */
@Mapper
public interface TeachPrepareRecordMapper extends BaseMapper<TeachPrepareRecord> {

    /**
     * 轮询用：只取 status 单列。
     * <p>
     * 备课记录里五段材料都是 longtext，前端每两三秒轮询一次状态时不该整行捞回来。
     * 自定义 SQL 不走 MyBatis-Plus 的 {@code @TableLogic}，deleted 条件写死在语句里。
     *
     * @param recordId  备课记录ID
     * @param teacherId 备课教师ID（归属校验下推到 SQL）
     * @return 状态码；记录不存在或不属于该教师时返回 null
     */
    @Select("SELECT status FROM teach_prepare_record "
            + "WHERE id = #{recordId} AND teacher_id = #{teacherId} AND deleted = 0")
    Integer selectStatusByIdAndTeacher(@Param("recordId") Long recordId,
                                      @Param("teacherId") Long teacherId);
}
