package com.cat.simple.task.agent;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.serializer.Serializer;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.check_point.CheckPointSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;


public class CustomRedisSaver implements BaseCheckpointSaver {

    // ✅ 可自定义的前缀
    private final String checkpointPrefix;
    private final String threadMetaPrefix;
    private final String threadReversePrefix;
    private final String lockPrefix;

    private final Serializer<Checkpoint> checkpointSerializer;
    private final RedissonClient redisson;
    private final long ttl;
    private final TimeUnit ttlUnit;

    protected CustomRedisSaver(RedissonClient redisson, StateSerializer stateSerializer,
                               long ttl, TimeUnit ttlUnit, String namespace) {
        requireNonNull(redisson, "redisson cannot be null");
        requireNonNull(stateSerializer, "stateSerializer cannot be null");
        this.redisson = redisson;
        this.checkpointSerializer = new CheckPointSerializer(stateSerializer);
        this.ttl = ttl;
        this.ttlUnit = ttlUnit;

        // ✅ 基于 namespace 生成所有前缀
        String ns = namespace.endsWith(":") ? namespace : namespace + ":";
        this.checkpointPrefix = ns + "graph:checkpoint:content:";
        this.threadMetaPrefix = ns + "graph:thread:meta:";
        this.threadReversePrefix = ns + "graph:thread:reverse:";
        this.lockPrefix = ns + "graph:checkpoint:lock:";
    }

    public static Builder builder() {
        return new Builder();
    }

    // ====== 以下方法与原版完全相同，仅将硬编码常量替换为实例字段 ======

    private String serializeCheckpoints(List<Checkpoint> checkpoints) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeInt(checkpoints.size());
            for (Checkpoint checkpoint : checkpoints) {
                checkpointSerializer.write(checkpoint, oos);
            }
            oos.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        }
    }

    private LinkedList<Checkpoint> deserializeCheckpoints(String content)
            throws IOException, ClassNotFoundException {
        if (content == null || content.isEmpty()) return new LinkedList<>();
        byte[] bytes = Base64.getDecoder().decode(content);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            int size = ois.readInt();
            LinkedList<Checkpoint> list = new LinkedList<>();
            for (int i = 0; i < size; i++) list.add(checkpointSerializer.read(ois));
            return list;
        }
    }

    private String getOrCreateThreadId(String threadName) {
        String metaKey = threadMetaPrefix + threadName;
        RMap<String, String> meta = redisson.getMap(metaKey);
        String threadId = meta.get("thread_id");
        String isReleased = meta.get("is_released");
        if (threadId != null && !"true".equals(isReleased)) return threadId;

        String newThreadId = UUID.randomUUID().toString();
        meta.put("thread_id", newThreadId);
        meta.put("is_released", "false");
        if (ttl > 0) meta.expire(java.time.Duration.ofMillis(ttlUnit.toMillis(ttl)));

        String reverseKey = threadReversePrefix + newThreadId;
        RMap<String, String> reverse = redisson.getMap(reverseKey);
        reverse.put("thread_name", threadName);
        reverse.put("is_released", "false");
        if (ttl > 0) reverse.expire(java.time.Duration.ofMillis(ttlUnit.toMillis(ttl)));
        return newThreadId;
    }

    private String getActiveThreadId(String threadName) {
        RMap<String, String> meta = redisson.getMap(threadMetaPrefix + threadName);
        String threadId = meta.get("thread_id");
        String isReleased = meta.get("is_released");
        return (threadId != null && !"true".equals(isReleased)) ? threadId : null;
    }

    @Override
    public Collection<Checkpoint> list(RunnableConfig config) {
        String threadName = config.threadId()
                .orElseThrow(() -> new IllegalArgumentException("threadId is not allow null"));
        RLock lock = redisson.getLock(lockPrefix + threadName);
        boolean tryLock = false;
        try {
            tryLock = lock.tryLock(500, TimeUnit.MILLISECONDS);
            if (!tryLock) return List.of();
            String threadId = getActiveThreadId(threadName);
            if (threadId == null) return List.of();
            String content = redisson.<String>getBucket(checkpointPrefix + threadId).get();
            return deserializeCheckpoints(content);
        } catch (InterruptedException e) { throw new RuntimeException(e); }
          catch (IOException | ClassNotFoundException e) { throw new RuntimeException(e); }
          finally { if (lock.isHeldByCurrentThread()) lock.unlock(); }
    }

    @Override
    public Optional<Checkpoint> get(RunnableConfig config) {
        String threadName = config.threadId()
                .orElseThrow(() -> new IllegalArgumentException("threadId isn't allow null"));
        RLock lock = redisson.getLock(lockPrefix + threadName);
        boolean tryLock = false;
        try {
            tryLock = lock.tryLock(500, TimeUnit.MILLISECONDS);
            if (!tryLock) return Optional.empty();
            String threadId = getActiveThreadId(threadName);
            if (threadId == null) return Optional.empty();
            String content = redisson.<String>getBucket(checkpointPrefix + threadId).get();
            LinkedList<Checkpoint> checkpoints = deserializeCheckpoints(content);
            if (config.checkPointId().isPresent()) {
                return config.checkPointId().flatMap(id -> checkpoints.stream()
                        .filter(cp -> cp.getId().equals(id)).findFirst());
            }
            return getLast(checkpoints, config);
        } catch (InterruptedException e) { throw new RuntimeException(e); }
          catch (IOException | ClassNotFoundException e) { throw new RuntimeException(e); }
          finally { if (lock.isHeldByCurrentThread()) lock.unlock(); }
    }

    @Override
    public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
        String threadName = config.threadId()
                .orElseThrow(() -> new IllegalArgumentException("threadId isn't allow null"));
        RLock lock = redisson.getLock(lockPrefix + threadName);
        boolean tryLock = false;
        try {
            tryLock = lock.tryLock(3, TimeUnit.SECONDS);
            if (!tryLock) throw new RuntimeException("Failed to acquire lock for thread: " + threadName);
            String threadId = getOrCreateThreadId(threadName);
            RBucket<String> bucket = redisson.getBucket(checkpointPrefix + threadId);
            LinkedList<Checkpoint> checkpoints = deserializeCheckpoints(bucket.get());
            if (config.checkPointId().isPresent()) {
                String cpId = config.checkPointId().get();
                int index = IntStream.range(0, checkpoints.size())
                        .filter(i -> checkpoints.get(i).getId().equals(cpId))
                        .findFirst()
                        .orElseThrow(() -> new NoSuchElementException(
                                format("Checkpoint with id %s not found!", cpId)));
                checkpoints.set(index, checkpoint);
            } else {
                checkpoints.push(checkpoint);
            }
            bucket.set(serializeCheckpoints(checkpoints));
            if (ttl > 0) bucket.expire(java.time.Duration.ofMillis(ttlUnit.toMillis(ttl)));
            return RunnableConfig.builder(config).checkPointId(checkpoint.getId()).build();
        } catch (InterruptedException e) { throw new RuntimeException(e); }
          catch (IOException | ClassNotFoundException e) { throw new RuntimeException(e); }
          finally { if (lock.isHeldByCurrentThread()) lock.unlock(); }
    }

    @Override
    public Tag release(RunnableConfig config) throws Exception {
        String threadName = config.threadId()
                .orElseThrow(() -> new IllegalArgumentException("threadId is not allow null"));
        RLock lock = redisson.getLock(lockPrefix + threadName);
        boolean tryLock = false;
        try {
            tryLock = lock.tryLock(3, TimeUnit.SECONDS);
            if (!tryLock) throw new RuntimeException("Failed to acquire lock for thread: " + threadName);
            RMap<String, String> meta = redisson.getMap(threadMetaPrefix + threadName);
            String threadId = meta.get("thread_id");
            if (threadId == null) throw new IllegalStateException("Thread not found: " + threadName);
            meta.put("is_released", "true");
            RMap<String, String> reverse = redisson.getMap(threadReversePrefix + threadId);
            if (reverse != null) reverse.put("is_released", "true");
            String content = redisson.<String>getBucket(checkpointPrefix + threadId).get();
            Collection<Checkpoint> checkpoints = deserializeCheckpoints(content);
            return new Tag(threadName, checkpoints);
        } catch (InterruptedException e) { throw new RuntimeException(e); }
          catch (IOException | ClassNotFoundException e) { throw new RuntimeException(e); }
          finally { if (lock.isHeldByCurrentThread()) lock.unlock(); }
    }

    // ====== Builder ======

    public static class Builder {
        private RedissonClient redisson;
        private StateSerializer stateSerializer;
        private long ttl = -1;
        private TimeUnit ttlUnit = TimeUnit.SECONDS;
        private String namespace = "graph"; // ✅ 默认保持与原框架兼容

        public Builder redisson(RedissonClient redisson) { this.redisson = redisson; return this; }
        public Builder stateSerializer(StateSerializer s) { this.stateSerializer = s; return this; }
        public Builder ttl(long ttl, TimeUnit unit) {
            if (unit == null) throw new IllegalArgumentException("ttlUnit cannot be null");
            if (ttl == 0 || ttl < -1) throw new IllegalArgumentException("ttl must be positive or -1");
            this.ttl = ttl; this.ttlUnit = unit; return this;
        }
        /** ✅ 新增：设置 Key 命名空间（前缀） */
        public Builder namespace(String namespace) { this.namespace = namespace; return this; }

        public CustomRedisSaver build() {
            if (redisson == null) throw new IllegalArgumentException("redisson cannot be null");
            if (stateSerializer == null) stateSerializer = StateGraph.DEFAULT_JACKSON_SERIALIZER;
            return new CustomRedisSaver(redisson, stateSerializer, ttl, ttlUnit, namespace);
        }
    }
}