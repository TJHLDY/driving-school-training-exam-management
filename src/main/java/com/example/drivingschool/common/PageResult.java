package com.example.drivingschool.common;

import java.util.List;

public record PageResult<T>(List<T> list, long total, int page, int pageSize) {
}
