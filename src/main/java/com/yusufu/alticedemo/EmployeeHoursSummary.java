package com.yusufu.alticedemo;

import java.math.BigDecimal;

public record EmployeeHoursSummary(long employeeId, String employeeName, BigDecimal totalHours) {}