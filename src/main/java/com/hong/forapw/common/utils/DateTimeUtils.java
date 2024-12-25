package com.hong.forapw.common.utils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateTimeUtils {

    private DateTimeUtils() {
    }

    public static final DateTimeFormatter DATE_HOUR_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");
    public static final DateTimeFormatter YEAR_HOUR_DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static String formatDate(Date date, String format) {
        return new SimpleDateFormat(format).format(date);
    }

    public static String formatLocalDateTime(LocalDateTime localDateTime, DateTimeFormatter formatter) {
        return localDateTime.format(formatter);
    }
}