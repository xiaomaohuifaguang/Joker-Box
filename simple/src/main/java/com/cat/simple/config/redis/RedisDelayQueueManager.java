package com.cat.simple.config.redis;

import com.cat.simple.config.redis.core.RedisDelayEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * 通用 Redis 延迟队列管理器（Redisson RDelayedQueue 封装）。
 *
 * <h3>语义保证</h3>
 * <ul>
 *   <li>到点精准触发（内部 zset 按到期时间调度，非 key 过期采样）</li>
 *   <li>延迟时长任意（每条消息独立 delay，与 RocketMQ 4.x 的固定档位不同）</li>
 *   <li>多节点部署：各节点 take() 竞争消费，一条消息只被一个节点处理</li>
 *   <li>节点全停期间消息留在 Redis，重启后集中补发（迟到但必达）</li>
 *   <li>take 之后、handler 执行前宕机会丢一条 —— 无 ACK，业务方需自备对账兜底</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * // 投递
 * redisDelayQueueManager.offer("order:cancel", "order:1001", event, 30, TimeUnit.MINUTES);
 *
 * // 取消（业务提前完成）
 * redisDelayQueueManager.cancel("order:cancel", "order:1001");
 *
 * // 订阅（应用启动时调用一次，每队列一次）
 * redisDelayQueueManager.subscribe("order:cancel", OrderCancelEvent.class,
 *         (key, event) -> orderService.autoCancel(event));
 * }</pre>
 *
 * <h3>设计说明</h3>
 * <ul>
 *   <li>payload 走 JSON 字符串：绕开 Redisson codec 的类型信息问题，换 codec 配置不影响存量消息</li>
 *   <li>RDelayedQueue 必须常驻持有（搬运线程活在客户端对象里，被 GC 就停摆），故按队列名缓存于本单例</li>
 *   <li>每队列单线程串行消费：handler 内无需考虑并发；吞吐不足再扩展为线程池</li>
 * </ul>
 *
 * <h3>关于 RDelayedQueue 废弃（决策记录）</h3>
 * Redisson 3.46.0 起标记废弃，官方建议迁移 RReliableQueue（带 ACK/DLQ）。
 * 但 RReliableQueue 为 PRO 付费功能，开源版 getReliableQueue() 直接抛
 * UnsupportedOperationException，故本项目压着废弃警告继续使用 RDelayedQueue：
 * <ol>
 *   <li>可靠性已由业务防重 + 定期扫表对账兜底，"take 后丢一条"窗口可接受</li>
 *   <li>cancel-by-key 语义依赖 RDelayedQueue.remove()，PRO 方案的墓碑式取消反而绕</li>
 * </ol>
 */
@Slf4j
@Component
public class RedisDelayQueueManager {

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 队列名 → 持有者（常驻，勿释放）
     */
    private final Map<String, QueueHolder> holders = new ConcurrentHashMap<>();

    /**
     * 消费线程池（每队列一个线程，串行消费）
     */
    private final ExecutorService consumerExecutor =
            Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "delay-queue-consumer");
                t.setDaemon(false);
                return t;
            });

    /**
     * 获取队列持有者（懒初始化）
     */
    private QueueHolder holder(String queue) {
        return holders.computeIfAbsent(queue, name -> {
            RBlockingQueue<RedisDelayEnvelope> bq = redissonClient.getBlockingQueue(name);
            RDelayedQueue<RedisDelayEnvelope> dq = redissonClient.getDelayedQueue(bq);
            log.info("[DelayQueue] 队列初始化: {}", name);
            return new QueueHolder(bq, dq);
        });
    }

    /**
     * 投递延迟消息。
     *
     * @param queue   队列名（业务隔离，命名建议 "业务域:动作"，如 "order:cancel"）
     * @param key     业务键（同队列内唯一；取消时唯一依据）
     * @param payload 业务对象，任意可 JSON 序列化的 POJO（需无参构造 + getter/setter）
     * @param delay   延迟时长（任意值，每条消息独立计时）
     * @throws IllegalStateException 序列化或投递失败时抛出（由调用方决定是否回滚业务事务）
     */
    public <T> void offer(String queue, String key, T payload, long delay, TimeUnit unit) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            holder(queue).delayedQueue.offer(new RedisDelayEnvelope(key, json), delay, unit);
            log.debug("[DelayQueue] 投递 queue={} key={} delay={}{}", queue, key, delay, unit);
        } catch (Exception e) {
            log.error("[DelayQueue] 投递失败 queue={} key={}", queue, key, e);
            throw new IllegalStateException("延迟消息投递失败: " + key, e);
        }
    }

    /**
     * 按业务键取消未到期的消息。
     * <p>仅凭 key 匹配（DelayEnvelope.equals 只比较 key），无需重建完整 payload。
     * <p>注意：若消息恰已到点出队，则删不掉 —— 消费端应校验业务状态兜底。
     *
     * @return true=成功移除；false=无匹配（未投递过 / 已出队 / 已消费）
     */
    public boolean cancel(String queue, String key) {
        try {
            boolean removed = holder(queue).delayedQueue.remove(new RedisDelayEnvelope(key, null));
            log.debug("[DelayQueue] 取消 queue={} key={} removed={}", queue, key, removed);
            return removed;
        } catch (Exception e) {
            log.warn("[DelayQueue] 取消异常 queue={} key={}", queue, key, e);
            return false;
        }
    }

    /**
     * 订阅队列：注册常驻消费线程，到点的消息逐条回调。
     * <p>在应用启动阶段调用（如 ApplicationRunner），每队列调用一次；
     * 重复调用会起多个消费线程（也能正常工作，只是没必要）。
     *
     * <h3>消费流程</h3>
     * <pre>
     * take() → 反序列化 → handler
     *   ├─ handler 正常返回 → 消息终结 ✅
     *   └─ handler 抛异常 → 捕获记日志，消息即丢弃 ⚠️
     *        （无重投机制，重要业务需在 handler 内自行落库/重试，
     *          可靠性兜底依赖业务防重表 + 定期扫表对账）
     * </pre>
     *
     * @param payloadType payload 还原的目标类型
     * @param handler     消费回调 (key, payload)；建议校验业务状态保证幂等
     */
    public <T> void subscribe(String queue, Class<T> payloadType, BiConsumer<String, T> handler) {
        QueueHolder h = holder(queue);
        consumerExecutor.submit(() -> {
            log.info("[DelayQueue] 消费启动 queue={} type={}", queue, payloadType.getSimpleName());
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    RedisDelayEnvelope envelope = h.blockingQueue.take(); // 阻塞等，多节点竞争
                    T payload = objectMapper.readValue(envelope.getPayloadJson(), payloadType);
                    handler.accept(envelope.getKey(), payload);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // 无重投：本条消息丢弃。重要业务在 handler 内自行处理失败（落库/告警/补偿）
                    log.error("[DelayQueue] 消费异常, 消息丢弃 queue={}", queue, e);
                }
            }
            log.info("[DelayQueue] 消费退出 queue={}", queue);
        });
    }

    /**
     * 优雅停机：停消费线程，停各队列搬运调度
     */
    @PreDestroy
    public void shutdown() {
        consumerExecutor.shutdownNow();
        holders.values().forEach(h -> h.delayedQueue.destroy());
        log.info("[DelayQueue] 已关闭, 共 {} 个队列", holders.size());
    }

    /**
     * 队列持有者：目标队列与延迟队列必须成对常驻
     */
    private record QueueHolder(RBlockingQueue<RedisDelayEnvelope> blockingQueue,
                               RDelayedQueue<RedisDelayEnvelope> delayedQueue) {
    }
}
