package com.amlve.agentqa.mapper;

import com.amlve.agentqa.tableEnt.QaRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 问答记录 Mapper
 */
public interface QaRecordMapper extends BaseMapper<QaRecord> {

    /**
     * 查询某会话已落库的最大轮次，用于 Redis 轮次计数失效时兜底。
     * <p>
     * round_no 由 Redis {@code INCR} 生成，Redis 重启或 key 过期（TTL 30 分钟）后会从 1 重来，
     * 同一 session 就会出现重复轮次、历史查询还原对话顺序时两问同号。
     * 任务书 6.18.3 给了两种收口方式（加唯一键 / DB 兜底校正），本表结构已定且不带唯一键，
     * 故选后者：INCR 返回 1 时先问一次本方法，把计数器补到真实水位再往下走。
     * 自定义 SQL 不会被 MyBatis-Plus 的 {@code @TableLogic} 自动过滤，deleted 条件写死在语句里。
     *
     * @param sessionId 会话ID
     * @return 最大轮次；该会话还没有记录时返回 null
     */
    @Select("SELECT MAX(round_no) FROM qa_record WHERE session_id = #{sessionId} AND deleted = 0")
    Integer selectMaxRoundNo(@Param("sessionId") String sessionId);
}
