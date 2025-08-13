package com.yusufu.alticedemo;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class EmployeeReportJob {

    private final EmployeeReportService report;
    private final HoursWebSocketHandler wsHandler;
    private final ZoneId zone = ZoneId.of("America/New_York");

    public EmployeeReportJob(EmployeeReportService report, HoursWebSocketHandler wsHandler) {
        this.report = report;
        this.wsHandler = wsHandler;
    }

    @Scheduled(cron = "0 */1 * * * *", zone = "America/New_York")
    public void minuteLyWeekReport() {
        LocalDate prevMon = LocalDate.now(zone).minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate prevSun = prevMon.plusDays(6);
        var rows = report.callSummarize(prevMon, prevSun);

        rows.forEach(r -> System.out.println("employee_id=" + r.employeeId() + " total_hours=" + r.totalHours()));
        wsHandler.broadcastHours(rows);
    }
}
