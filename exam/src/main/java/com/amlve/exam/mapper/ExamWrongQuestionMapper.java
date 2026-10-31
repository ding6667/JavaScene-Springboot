package com.amlve.exam.mapper;

import com.amlve.exam.tableEnt.ExamWrongQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 错题归档 Mapper。
 */
public interface ExamWrongQuestionMapper extends BaseMapper<ExamWrongQuestion> {

    /**
     * 复活一条被逻辑删除的错题记录并累计一次错误。
     * <p>
     * 唯一键 (user_id, snapshot_id, source) 会把逻辑删除的行也算进去，
     * 学生删过错题后又错一次时必须走 UPDATE 复活，否则 INSERT 会撞唯一键。
     * 这里显式写 deleted = 0，绕开 MyBatis-Plus 的逻辑删除条件。
     */
    @Update("update exam_wrong_question"
            + " set deleted = 0, wrong_count = wrong_count + 1, last_answer = #{lastAnswer},"
            + " mastered = 0, last_wrong_time = #{now}"
            + " where user_id = #{userId} and snapshot_id = #{snapshotId} and source = #{source}")
    int revive(@Param("userId") Long userId,
               @Param("snapshotId") Long snapshotId,
               @Param("source") Integer source,
               @Param("lastAnswer") String lastAnswer,
               @Param("now") LocalDateTime now);
}
