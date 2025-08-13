package com.yusufu.alticedemo;

import org.hibernate.dialect.OracleTypes;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class EmployeeReportService {

    private final JdbcTemplate jdbcTemplate;

    public EmployeeReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<EmployeeHoursSummary> callSummarize(LocalDate start, LocalDate end) {
        var call = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("SUMMARIZE_HOURS")
                .declareParameters(
                        new SqlParameter("P_START_DATE", Types.DATE),
                        new SqlParameter("P_END_DATE", Types.DATE),
                        new SqlOutParameter("P_RC", OracleTypes.CURSOR,
                                (rs, i) -> new EmployeeHoursSummary(
                                        rs.getLong(1),
                                        rs.getString(2),
                                        rs.getBigDecimal(3)))
                )
                .withoutProcedureColumnMetaDataAccess();

        Map<String, Object> out = call.execute(new MapSqlParameterSource()
                .addValue("P_START_DATE", Date.valueOf(start))
                .addValue("P_END_DATE",   Date.valueOf(end)));

        @SuppressWarnings("unchecked")
        List<EmployeeHoursSummary> rows = (List<EmployeeHoursSummary>) out.get("P_RC");
        return rows;
    }
}
