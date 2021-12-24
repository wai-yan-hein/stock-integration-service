package com.cv.integration.common;

import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

@Slf4j
public class Util1 {
    public static double getDouble(Object obj) {
        return obj == null ? 0 : Double.parseDouble(obj.toString());
    }

    public static Date getTodayDate() {
        return Calendar.getInstance().getTime();
    }

    public static Date toDate(String sqlDate) {
        Date date = null;
        try {
            date = new SimpleDateFormat("yyyy-MM-dd").parse(sqlDate);
        } catch (ParseException e) {
            log.error(String.format("toDate : %s", e));
        }
        return date;
    }
}
