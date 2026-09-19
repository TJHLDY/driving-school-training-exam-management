package com.example.drivingschool.common;

import com.example.drivingschool.exception.BusinessException;
import org.springframework.http.HttpStatus;

public final class PageUtils {

    private PageUtils() {
    }

    public static int page(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    public static int pageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    public static long offset(int page, int pageSize) {
        return (long) (page - 1) * pageSize;
    }

    public static void requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, field + "必须为正整数");
        }
    }
}
