package com.cat.simple.config.redis;

import com.cat.simple.config.cache.CacheService;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 延迟队列联调测试：仅 dev 环境生效，观察控制台日志验证。
 *
 * <h3>预期日志时序</h3>
 * <pre>
 * t=0s   消费启动（subscribe）
 * t=0s   投递 3 条 + 取消 msgC
 * t=5s   收到 msgA（正常消费）
 * t=10s  收到 msgB（故意异常 → 无重投，本条丢弃，日志可见消费异常）
 * t=15s  msgC 无日志（已被 cancel 真实删除，队列里根本没有这条了）
 * </pre>
 */
@Slf4j
@Component
public class TestDelayQueue implements ApplicationRunner {

    @Resource
    private RedisDelayQueueManager redisDelayQueueManager;
    @Resource
    private CacheService cacheService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String queue = cacheService.makeKey("test:delay");

        // ① 订阅（必须在投递之前，否则消息到点无人消费，留到下一次启动）
        redisDelayQueueManager.subscribe(queue, TestEvent.class, (key, event) -> {
            log.info("[TEST] 收到消息 key={} title={}", key, event.getTitle());

            // msgB 故意失败：验证异常时消息被丢弃（无重投 —— 这就是无 ACK 版本的行为）
            if ("msgB".equals(key)) {
                throw new RuntimeException("[TEST] msgB 故意失败");
            }
        });
        log.info("[TEST] 订阅完成 queue={}", queue);

        // ② 投递三条
        TestEvent a = new TestEvent("消息A");
        TestEvent b = new TestEvent("消息B");
        TestEvent c = new TestEvent("消息C");
        redisDelayQueueManager.offer(queue, "msgA", a, 5, TimeUnit.SECONDS);
        redisDelayQueueManager.offer(queue, "msgB", b, 10, TimeUnit.SECONDS);
        redisDelayQueueManager.offer(queue, "msgC", c, 15, TimeUnit.SECONDS);
        log.info("[TEST] 投递完成: msgA(5s) / msgB(10s,将异常) / msgC(15s,将取消)");

        // ③ 立刻取消 msgC（投递后马上取消，15 秒内完成即可）
        boolean removed = redisDelayQueueManager.cancel(queue, "msgC");
        log.info("[TEST] cancel msgC 结果: {}（预期 true，真实从 Redis 删除）", removed);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestEvent {
        private String title;
    }
}
