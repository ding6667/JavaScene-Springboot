package com.amlve.agentqa.service;

import com.amlve.agentqa.dto.context.ContextMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 多轮对话上下文管理器（Redis，问答双层存储的"短期层"）
 * <p>
 * Key 口径与 knowledge 的缓存（{@code knowledge:}）、登录黑名单共用同一实例，前缀必须错开：
 * <ul>
 *     <li>{@code qa:session:{userId}:{chapterId}} —— 最近若干轮消息，List 结构，chapterId 为空时用 all</li>
 *     <li>{@code qa:round:{sessionId}} —— 会话轮次计数器，INCR 递增</li>
 * </ul>
 * 两组 key 的 TTL 都是 30 分钟：不活跃即自动清理，且"上下文过期"本就等价于"这段对话结束了"。
 * <p>
 * <b>用 Redis List（RPUSH + LTRIM + EXPIRE）而不是一个 JSON 字符串</b>：
 * 读-改-写整体覆盖在并发下会互相吞掉对方那一轮（同一学生双端打开、或连问两条），
 * 而 RPUSH 是服务端原子追加。对应任务书 6.18.4。
 * <p>
 * <b>所有方法都不向外抛异常</b>：上下文是可丢弃数据，Redis 挂了顶多退化成单轮问答，
 * 不能把学生提问这条主流程一起打断（任务书 11.1 的降级要求）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QaContextManager {

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    /**
     * 上下文 key 前缀
     */
    private static final String CONTEXT_KEY_PREFIX = "qa:session:";

    /**
     * 轮次计数 key 前缀
     */
    private static final String ROUND_KEY_PREFIX = "qa:round:";

    /**
     * TTL（分钟）
     */
    private static final long TTL_MINUTES = 30L;

    /**
     * 最多保留轮次：再多会挤占提示词空间并费 token，且答疑是"章节定点"场景，远了的历史价值低
     */
    private static final int MAX_ROUNDS = 5;

    /**
     * 上下文字符数上限（粗略 token 估算），超出从最旧的一条开始丢
     */
    private static final int MAX_CONTEXT_CHARS = 8000;

    /**
     * 一轮含 user + assistant 两条消息，故 List 长度上限是轮次的 2 倍
     */
    private static final int MAX_MESSAGES = MAX_ROUNDS * 2;

    /**
     * 读取上下文（最旧→最新顺序）
     *
     * @param userId    学生ID
     * @param chapterId 章节ID，可为 null
     * @return 上下文消息列表；Redis 不可用或无历史时返回空列表（退化为单轮问答）
     */
    public List<ContextMessage> getContext(Long userId, Long chapterId) {
        String key = buildContextKey(userId, chapterId);
        try {
            List<String> raw = redisTemplate.opsForList().range(key, 0, -1);
            if (raw == null || raw.isEmpty()) {
                return new ArrayList<>();
            }
            List<ContextMessage> messages = new ArrayList<>(raw.size());
            for (String json : raw) {
                ContextMessage message = readMessage(json);
                if (message != null) {
                    messages.add(message);
                }
            }
            return messages;
        } catch (Exception e) {
            log.warn("读取对话上下文失败，本轮按单轮问答处理: key={}, err={}", key, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 追加本轮问答到上下文（user 与 assistant 各一条）
     * <p>
     * 追加后依次做：按条数裁剪、按字符数裁剪、续期。任一步失败只告警，
     * 最坏结果是这一轮上下文没记住，下一轮仍能从"无历史"继续。
     *
     * @param userId    学生ID
     * @param chapterId 章节ID，可为 null
     * @param question  学生问题
     * @param answer    AI 回答
     */
    public void appendRound(Long userId, Long chapterId, String question, String answer) {
        String key = buildContextKey(userId, chapterId);
        try {
            long now = System.currentTimeMillis() / 1000;
            redisTemplate.opsForList().rightPush(key, writeMessage(new ContextMessage("user", question, now)));
            redisTemplate.opsForList().rightPush(key, writeMessage(new ContextMessage("assistant", answer, now)));
            // 负索引从头算：只保留最近 MAX_MESSAGES 条
            redisTemplate.opsForList().trim(key, -MAX_MESSAGES, -1);
            trimByChars(key);
            redisTemplate.expire(key, TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入对话上下文失败: key={}, err={}", key, e.getMessage());
        }
    }

    /**
     * 取本轮轮次序号
     * <p>
     * 正常路径是 Redis INCR 原子递增；返回值仍为 1 时先向 DB 问一次真实水位再补上去，
     * 否则 Redis 重启 / key 过期后同一会话会从 1 重新计数，历史里会出现两条"第 1 轮"。
     *
     * @param sessionId     会话ID（非空）
     * @param dbMaxRoundNo  该会话在库里已有的最大轮次（无记录传 0），只在 Redis 计数疑似归零时才会被用到
     * @return 本轮轮次，从 1 递增
     */
    public int nextRoundNo(String sessionId, int dbMaxRoundNo) {
        String key = ROUND_KEY_PREFIX + sessionId;
        try {
            Long round = redisTemplate.opsForValue().increment(key);
            if (round == null) {
                // 拿不到计数时退回 DB 水位，至少保证本条记录有个轮次
                log.warn("Redis 轮次计数无返回值，改用库内最大轮次: sessionId={}, dbMax={}", sessionId, dbMaxRoundNo);
                return dbMaxRoundNo + 1;
            }
            if (round == 1L && dbMaxRoundNo > 0) {
                // 计数器是新起的而库里已有历史：把差值一次性补上，返回值即真实轮次
                Long corrected = redisTemplate.opsForValue().increment(key, dbMaxRoundNo);
                round = corrected == null ? dbMaxRoundNo + 1L : corrected;
                log.info("Redis 轮次计数已从库内水位补齐: sessionId={}, roundNo={}", sessionId, round);
            }
            redisTemplate.expire(key, TTL_MINUTES, TimeUnit.MINUTES);
            return round.intValue();
        } catch (Exception e) {
            log.warn("Redis 轮次计数失败，改用库内最大轮次: sessionId={}, err={}", sessionId, e.getMessage());
            return dbMaxRoundNo + 1;
        }
    }

    /**
     * 清空某学生某章节的上下文（"换个话题"用，当前无接口调用，留给后续重置入口）
     *
     * @param userId    学生ID
     * @param chapterId 章节ID，可为 null
     */
    public void clearContext(Long userId, Long chapterId) {
        try {
            redisTemplate.delete(buildContextKey(userId, chapterId));
        } catch (Exception e) {
            log.warn("清空对话上下文失败: userId={}, chapterId={}, err={}", userId, chapterId, e.getMessage());
        }
    }

    /**
     * 上下文字符数超限（且仍有历史可丢）时，从最旧的一条开始弹出
     * <p>
     * 至少留 2 条（一问一答），否则本轮刚说完就把自己忘了。
     */
    private void trimByChars(String key) {
        List<String> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null || raw.size() <= 2) {
            return;
        }
        int totalChars = 0;
        for (String json : raw) {
            ContextMessage message = readMessage(json);
            totalChars += message == null || message.getContent() == null
                    ? 0 : message.getContent().length();
        }
        int index = 0;
        while (totalChars > MAX_CONTEXT_CHARS && raw.size() - index > 2) {
            ContextMessage oldest = readMessage(raw.get(index));
            totalChars -= oldest == null || oldest.getContent() == null
                    ? 0 : oldest.getContent().length();
            index++;
        }
        if (index > 0) {
            redisTemplate.opsForList().trim(key, index, -1);
            log.debug("上下文超 {} 字符，丢弃最旧 {} 条", MAX_CONTEXT_CHARS, index);
        }
    }

    private String buildContextKey(Long userId, Long chapterId) {
        return CONTEXT_KEY_PREFIX + userId + ":" + (chapterId != null ? chapterId : "all");
    }

    private String writeMessage(ContextMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            throw new IllegalStateException("序列化上下文消息失败", e);
        }
    }

    /**
     * 单条消息反序列化：格式异常的元素跳过，不能让一条脏数据废掉整段上下文
     */
    private ContextMessage readMessage(String json) {
        try {
            return objectMapper.readValue(json, ContextMessage.class);
        } catch (Exception e) {
            log.warn("上下文消息格式异常已跳过: {}", json);
            return null;
        }
    }
}
