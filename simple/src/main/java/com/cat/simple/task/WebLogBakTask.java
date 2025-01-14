package com.cat.simple.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.common.entity.WebLog;
import com.cat.common.utils.datetime.DateTimeUtils;
import com.cat.simple.mapper.WebLogMapper;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WebLogBakTask {

    @Resource
    private WebLogMapper webLogMapper;

    @Scheduled(cron = "0 0 23 * * ?")
    public void bak() {
        String formatStrByLocalDate = DateTimeUtils.getFormatStrByLocalDate(DateTimeUtils.getLocalDateByDay(-15), DateTimeUtils.DATE_FORMAT_Y_M_D);
        webLogMapper.bak(formatStrByLocalDate);
        webLogMapper.delete(new LambdaQueryWrapper<WebLog>().lt(WebLog::getEndTime, formatStrByLocalDate));
    }



}
