package com.cat.simple.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.WebLog;
import com.cat.common.utils.datetime.DateTimeUtils;
import com.cat.simple.log.mapper.WebLogMapper;
import jakarta.annotation.Resource;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class WebLogBakTask {

    @Resource
    private WebLogMapper webLogMapper;

//    @Scheduled(cron = "0 0 23 * * ?")
    @Scheduled(initialDelay = 10, fixedDelay = 365 * 24 * 60 * 60, timeUnit = TimeUnit.SECONDS)
    @SchedulerLock(name = "WebLogBakTask.bak", lockAtMostFor = "30m")
    public void bak() {
        String formatStrByLocalDate = DateTimeUtils.getFormatStrByLocalDate(DateTimeUtils.getLocalDateByDay(-1), DateTimeUtils.DATE_FORMAT_Y_M_D);
        webLogMapper.bak(formatStrByLocalDate);
        webLogMapper.delete(new LambdaQueryWrapper<WebLog>().lt(WebLog::getEndTime, formatStrByLocalDate));
    }



}
