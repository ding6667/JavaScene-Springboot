package com.amlve.knowledge.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * knowledge 模块缓存配置（章节树等低频变更、高频读取的元数据）
 * <p>
 * 单容器架构下本配置对全应用生效，故所有缓存名统一以 {@code knowledge:} 前缀错开，
 * 不与问答上下文（{@code qa:}）、登录黑名单共用 key 空间。
 * <p>
 * 两个刻意的取舍：
 * <ul>
 *     <li>值序列化用 JSON 而非 JDK 默认序列化——DTO 不必 implements Serializable，
 *         Redis Desktop 里也能直接读懂内容便于排查；JSON 需带类型信息（activateDefaultTyping），
 *         否则 {@code List<ChapterTreeDTO>} 读回来会退化成 {@code List<LinkedHashMap>}；</li>
 *     <li>配置了降级用的 {@link CacheErrorHandler}：Redis 不可用时读缓存只告警并按未命中处理，
 *         写/失效缓存失败也只告警。缓存是加速手段不是数据源，不能因为 Redis 挂了把
 *         章节树、上传这些主接口一起打断。</li>
 * </ul>
 */
@Slf4j
@org.springframework.context.annotation.Configuration
@EnableCaching
public class KnowledgeCacheConfig {

    /**
     * 章节树缓存名，key 为 courseId（缓存项完整 key 形如 {@code knowledge:chapterTree::1}）
     */
    public static final String CHAPTER_TREE_CACHE = "knowledge:chapterTree";

    /**
     * 章节基本不变，按任务书口径 TTL 取 1 小时
     */
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);

    /**
     * 章节树缓存管理器。
     * <p>
     * 用 {@code createCachePrefix} 而不是默认的 {@code cacheName::} 拼法，把缓存名里的冒号
     * 保留下来（{@code knowledge:chapterTree:1}），与 Redis 中其它业务 key 的层级风格一致。
     *
     * @param connectionFactory Spring Boot 依 {@code spring.data.redis.*} 自动装配的连接工厂
     * @return 全局唯一的缓存管理器
     */
    @Bean
    public RedisCacheManager knowledgeCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .computePrefixWith(cacheName -> cacheName + ":")
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(cacheObjectMapper())))
                // 未命中的 null 不写缓存：章节不存在时每次都查库，避免 null 占位把"章节后来建了"这种情况挡在缓存里
                .disableCachingNullValues();
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(configuration)
                // 有意不开 transactionAware()：它把失效动作挪到事务提交后执行，而提交后回调里的异常
                // 不受下面的 CacheErrorHandler 兜底，Redis 一挂就会把已提交成功的 chapter/save 变成 500。
                // 代价是 @CacheEvict 可能略早于事务提交，提交窗口内的并发读有极小概率把旧树写回缓存，
                // 由 1 小时 TTL 兜底——章节保存是低频运维动作，这里按"缓存可短期陈旧，接口不可断"取舍
                .build();
    }

    /**
     * 让 {@code @Cacheable} 在缓存异常时降级而不是抛异常打断业务接口
     */
    @Bean
    public CachingConfigurer knowledgeCachingConfigurer() {
        return new CachingConfigurer() {
            @Override
            public CacheErrorHandler errorHandler() {
                return new DegradingCacheErrorHandler();
            }
        };
    }

    /**
     * 缓存专用 ObjectMapper：与 MVC 的 HTTP 序列化配置隔离，只多开类型信息
     */
    private ObjectMapper cacheObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return mapper;
    }

    /**
     * 缓存降级处理器：读失败按未命中处理，写/失效失败仅告警
     * <p>
     * Spring 默认的 SimpleCacheErrorHandler 会把缓存异常原样抛出，等于把 Redis 变成
     * knowledge 接口的强依赖，这与"缓存只是加速"的定位不符。
     */
    static class DegradingCacheErrorHandler implements CacheErrorHandler {

        @Override
        public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
            log.warn("读取缓存失败，降级为直接查库: cache={}, key={}, err={}",
                    cache.getName(), key, exception.getMessage());
        }

        @Override
        public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
            log.warn("写入缓存失败，本次结果不缓存: cache={}, key={}, err={}",
                    cache.getName(), key, exception.getMessage());
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
            // 失效失败必须留下痕迹：缓存里会短期残留旧数据（TTL 到期后自愈）
            log.error("清除缓存失败，短期内可能读到旧数据: cache={}, key={}, err={}",
                    cache.getName(), key, exception.getMessage());
        }

        @Override
        public void handleCacheClearError(RuntimeException exception, Cache cache) {
            log.error("清空缓存失败，短期内可能读到旧数据: cache={}, err={}",
                    cache.getName(), exception.getMessage());
        }
    }
}
