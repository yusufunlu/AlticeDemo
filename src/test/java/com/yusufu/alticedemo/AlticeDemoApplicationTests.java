package com.yusufu.alticedemo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AlticeDemoApplicationTests {

    @Autowired
    EmployeeReportService report;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void multistagePipeline_AB_then_CD_then_F() throws Exception {
        try { jdbc.execute("DROP TABLE TEST_SUMMARY_INSERTS PURGE"); } catch (Exception ignored) {}
        jdbc.execute("""
        CREATE TABLE TEST_SUMMARY_INSERTS (
          employee_id   NUMBER,
          employee_name VARCHAR2(100),
          total_hours   NUMBER(12,2),
          source_task   VARCHAR2(1)
        )
        """);

        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            LocalDate aStart = LocalDate.parse("2025-08-01");
            LocalDate aEnd   = LocalDate.parse("2025-08-07");

            LocalDate bStart = LocalDate.parse("2025-08-08");
            LocalDate bEnd   = LocalDate.parse("2025-08-11");


            CompletableFuture<List<EmployeeHoursSummary>> A =
                    CompletableFuture.supplyAsync(() -> report.callSummarize(aStart, aEnd), pool);

            CompletableFuture<List<EmployeeHoursSummary>> B =
                    CompletableFuture.supplyAsync(() -> report.callSummarize(bStart, bEnd), pool);

            CompletableFuture<List<EmployeeHoursSummary>> AplusB = A.thenCombine(B, (ra, rb) -> {
                List<EmployeeHoursSummary> all = new ArrayList<>(ra.size() + rb.size());
                all.addAll(ra);
                all.addAll(rb);
                return all;
            });

            CompletableFuture<Integer> C = AplusB.thenApplyAsync(rows -> {
                var evens = rows.stream()
                        .filter(r -> r.employeeId() % 2 == 0)
                        .map(r -> new Object[]{ r.employeeId(), r.employeeName(), r.totalHours(), "C" })
                        .toList();
                if (evens.isEmpty()) return 0;
                int[] res = jdbc.batchUpdate(
                        "INSERT INTO TEST_SUMMARY_INSERTS (employee_id, employee_name, total_hours, source_task) VALUES (?,?,?,?)",
                        evens);
                return sum(res);
            }, pool);

            CompletableFuture<Integer> D = AplusB.thenApplyAsync(rows -> {
                var odds = rows.stream()
                        .filter(r -> r.employeeId() % 2 != 0)
                        .map(r -> new Object[]{ r.employeeId(), r.employeeName(), r.totalHours(), "D" })
                        .toList();
                if (odds.isEmpty()) return 0;
                int[] res = jdbc.batchUpdate(
                        "INSERT INTO TEST_SUMMARY_INSERTS (employee_id, employee_name, total_hours, source_task) VALUES (?,?,?,?)",
                        odds);
                return sum(res);
            }, pool);

            CompletableFuture<Integer> F = C.thenCombine(D, (cCount, dCount) -> {
                Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM TEST_SUMMARY_INSERTS", Integer.class);
                System.out.println("C inserted=" + cCount + " D inserted=" + dCount + " total=" + total);
                return Objects.requireNonNullElse(total, 0);
            });

            int totalInserted = F.get(120, TimeUnit.SECONDS);
            int expected = AplusB.get(120, TimeUnit.SECONDS).size();

            assertEquals(expected, totalInserted, "Total rows inserted should equal combined rows from A and B");
        } finally {
            pool.shutdownNow();
            try { jdbc.execute("DROP TABLE TEST_SUMMARY_INSERTS PURGE"); } catch (Exception ignored) {}
        }
    }

    private static int sum(int[] arr) {
        int s = 0;
        for (int v : arr) s += v;
        return s;
    }
    }


