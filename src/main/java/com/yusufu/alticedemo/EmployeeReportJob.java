package com.yusufu.alticedemo;
import org.hibernate.dialect.OracleTypes;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Types;
import java.time.*;
import java.util.List;
import java.util.Map;


@Service
public class HoursSummaryJob {

    private final JdbcTemplate jdbcTemplate;
    private final SimpMessagingTemplate messaging;
    private final ZoneId zone = ZoneId.of("America/New_York");

    public HoursSummaryJob(JdbcTemplate jdbcTemplate, SimpMessagingTemplate messaging) {
        this.jdbcTemplate = jdbcTemplate;
        this.messaging = messaging;
    }

    // every minute (demo) – summarize previous week and broadcast
    @Scheduled(cron = "0 */1 * * * *", zone = "America/New_York")
    public void minuteLyWeekReport() {
        LocalDate prevMon = LocalDate.now(zone).minusWeeks(1).with(DayOfWeek.MONDAY);
        LocalDate prevSun = prevMon.plusDays(6);
        List<Row> rows = callSummarize(prevMon, prevSun);
        // console (your existing print)
        rows.forEach(r -> System.out.println("employee_id=" + r.employeeId + " total_hours=" + r.totalHours));
        // push to WebSocket subscribers
        messaging.convertAndSend("/topic/hours", rows);
    }

    // allow controller or others to request any range
    public List<Row> callSummarize(LocalDate start, LocalDate end) {
        SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SUMMARIZE_HOURS")
                .declareParameters(
                        new SqlParameter("P_START_DATE", Types.DATE),
                        new SqlParameter("P_END_DATE", Types.DATE),
                        new SqlOutParameter("P_RC", OracleTypes.CURSOR,
                                (rs, i) -> new Row(rs.getLong(1), rs.getString(2), rs.getBigDecimal(3)))
                )
                .withoutProcedureColumnMetaDataAccess();

        Map<String, Object> out = call.execute(new MapSqlParameterSource()
                .addValue("P_START_DATE", Date.valueOf(start))
                .addValue("P_END_DATE",   Date.valueOf(end)));

        @SuppressWarnings("unchecked")
        List<Row> list = (List<Row>) out.get("P_RC");
        return list;
    }

    public record Row(long employeeId, String employeeName, BigDecimal totalHours) {}
}
