package com.amlve.knowledge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * knowledge 模块异步线程池配置
 * <p>
 * 知识库文档上传要在 Dify 侧轮询索引状态（最长 5 分钟），同步做会长时间占住 Tomcat 请求线程
 * 与数据库连接，故改为"接口秒回上传记录ID → 后台线程完成建档与留痕回写"。
 */
@Configuration
public class KnowledgeAsyncConfig {

    /**
     * 上传任务线程池 Bean 名，注入处用同名匹配，避免误用容器里的其它执行器
     */
    public static final String UPLOAD_EXECUTOR = "knowledgeUploadExecutor";

    /**
     * 同时在建的文档数上限。
     * <p>
     * 这里刻意做小并且用固定线程数而不是虚拟线程：线程数就是并发闸门——每个任务都要 sleep
     * 轮询索引状态，放大并发等于同时对 Dify 打出多路请求，还会把本地文档名相同的建档竞态放大。
     * 任务书 3.11 的"建议限制并发上传数量"由这道闸门落实。
     */
    private static final int UPLOAD_CONCURRENCY = 2;

    /**
     * 等待队列容量。队列满即拒绝并提示稍后重试，不无限堆积（每份文档最长占用 5 分钟，
     * 排到几百名开外的上传对用户已等同于失败，不如快速失败）
     */
    private static final int UPLOAD_QUEUE_CAPACITY = 20;

    /**
     * 停机时等待在途上传任务的时间上限（秒）。超过则中断任务，对应记录会停留在"处理中"，
     * 由 {@code /upload/list} 里 status=0 的记录人工核对
     */
    private static final int SHUTDOWN_AWAIT_SECONDS = 60;

    /**
     * 知识库上传任务线程池
     *
     * @return 固定 {@value #UPLOAD_CONCURRENCY} 线程、队列 {@value #UPLOAD_QUEUE_CAPACITY} 的执行器
     */
    @Bean(name = UPLOAD_EXECUTOR)
    public ThreadPoolTaskExecutor knowledgeUploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(UPLOAD_CONCURRENCY);
        executor.setMaxPoolSize(UPLOAD_CONCURRENCY);
        executor.setQueueCapacity(UPLOAD_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("knowledge-upload-");
        // 优雅停机：等在途任务写完留痕再退，否则记录永远停在"处理中"且 Dify 侧可能残留半成品文档
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(SHUTDOWN_AWAIT_SECONDS);
        return executor;
    }
}
