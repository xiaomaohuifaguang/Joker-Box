package com.cat.simple.config.process;

import com.cat.simple.config.cache.CacheKeyEnum;
import com.cat.simple.config.cache.CacheService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class ProcessCodeGenerator {

    @Resource
    private CacheService cacheService;

    private static final String PREFIX = "JB";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generate() {
        String date = LocalDate.now().format(DATE_FMT);
        long seq = cacheService.incr(CacheKeyEnum.PROCESS_CODE_REQ, date);
        return String.format("%s-%s-%04d", PREFIX, date, seq);
    }
}
