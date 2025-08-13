package com.yusufu.alticedemo;

import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/hours")
class HoursController {

    private final EmployeeReportService report;
    private final HoursWebSocketHandler wsHandler;
    private final JdbcTemplate jdbcTemplate;

    HoursController(EmployeeReportService report, HoursWebSocketHandler wsHandler, JdbcTemplate jdbcTemplate) {
        this.report = report;
        this.wsHandler = wsHandler;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/summarize")
    public List<EmployeeHoursSummary> run(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(name = "broadcast", defaultValue = "false") boolean broadcast) {

        var rows = report.callSummarize(LocalDate.parse(start), LocalDate.parse(end));
        if (broadcast) wsHandler.broadcastHours(rows);
        return rows;
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<StreamingResponseBody> exportLogs(
            @RequestParam String start,
            @RequestParam String end) {

        LocalDate s = LocalDate.parse(start);
        LocalDate e = LocalDate.parse(end);

        final String sql = """
            SELECT employee_id, work_date, hours_worked
              FROM work_logs
             WHERE work_date BETWEEN ? AND ?
             ORDER BY work_date, employee_id
            """;

        StreamingResponseBody body = outputStream -> {
            try (var writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
                writer.write("employee_id,work_date,hours_worked\n");
                jdbcTemplate.query(con -> {
                    PreparedStatement ps = con.prepareStatement(
                            sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                    ps.setFetchSize(1000);
                    ps.setDate(1, Date.valueOf(s));
                    ps.setDate(2, Date.valueOf(e));
                    return ps;
                }, rs -> {
                    try {
                        writer.write(rs.getLong(1) + "," + rs.getDate(2) + "," +
                                (rs.getBigDecimal(3) == null ? "0" : rs.getBigDecimal(3).toPlainString()) + "\n");
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
                writer.flush();
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"work_logs.csv\"")
                .contentType(MediaType.valueOf("text/csv"))
                .body(body);
    }
}
