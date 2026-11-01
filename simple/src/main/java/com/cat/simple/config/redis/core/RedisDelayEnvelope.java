package com.cat.simple.config.redis.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 延迟消息信封。
 * <p>key 是取消与追踪的唯一依据（同队列内需唯一）；
 * payloadJson 是业务对象序列化结果，消费端按声明的类型还原。
 * <p>equals/hashCode 只看 key —— cancel 时用 {@code new DelayEnvelope(key, null)} 即可匹配。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RedisDelayEnvelope implements Serializable {

    /** 业务键，如 "order:1001:cancel" */
    private String key;

    /** 业务对象 JSON */
    private String payloadJson;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RedisDelayEnvelope that)) return false;
        return key != null && key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key == null ? 0 : key.hashCode();
    }
}
