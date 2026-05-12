package com.cat.simple.config.process;

import com.cat.simple.config.redis.RedisService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class ProcessCodeGenerator {

    @Resource
    private RedisService redisService;

    private static final String PREFIX = "LC";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String REDIS_KEY_TEMPLATE = "process:code:seq:%s";

    public String generate() {
        String date = LocalDate.now().format(DATE_FMT);
        String redisKey = String.format(REDIS_KEY_TEMPLATE, date);
        long seq = redisService.incr(redisKey);
        return String.format("%s-%s-%04d", PREFIX, date, seq);
    }
}
