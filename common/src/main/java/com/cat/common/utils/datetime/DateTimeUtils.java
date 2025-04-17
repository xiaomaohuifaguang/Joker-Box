package com.cat.common.utils.datetime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {


    public static final String DATE_FORMAT_Y_M_D = "yyyy-MM-dd";
    public static final String DATE_FORMAT_YMD = "yyyyMMdd";


    public static LocalDate getLocalDateByDay(Integer day) {
        LocalDate today = LocalDate.now(); // 获取当前日期
        return day > 0 ? today.plusDays(day) : today.minusDays(Math.abs(day));
    }

    public static String getFormatStrByLocalDate(LocalDate localDate, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return localDate.format(formatter);
    }





}
