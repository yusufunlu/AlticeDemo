package com.yusufu.alticedemo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HoursWebSocketHandler extends TextWebSocketHandler {

    private final EmployeeReportService report;
    private final ObjectMapper mapper;

    private record Sess(WebSocketSession session, Instant lastSeen) {}
    private final Map<String, Sess> sessionMap = new ConcurrentHashMap<>();
    private static final long TIMEOUT_MILLIS = 30_000;

    public HoursWebSocketHandler(EmployeeReportService report, ObjectMapper mapper) {
        this.report = report;
        this.mapper = mapper;
    }

    @Override public void afterConnectionEstablished(WebSocketSession s) throws Exception {
        sessionMap.put(s.getId(), new Sess(s, Instant.now()));
        sendJson(s, Map.of("type","hello","message","connected"));
    }

    @Override protected void handleTextMessage(WebSocketSession s, TextMessage m) throws Exception {
        sessionMap.computeIfPresent(s.getId(), (id, v) -> new Sess(v.session(), Instant.now()));

        String payload = m.getPayload();
        if ("ping".equalsIgnoreCase(payload)) { s.sendMessage(new TextMessage("pong")); return; }

        Map<String, Object> msg;
        try { msg = mapper.readValue(payload, new TypeReference<Map<String,Object>>(){});
        } catch (Exception e) { sendJson(s, Map.of("type","error","message","invalid JSON")); return; }

        String type = Objects.toString(msg.get("type"), "");
        switch (type) {
            case "ping" -> sendJson(s, Map.of("type","pong"));
            case "summarize_hours" -> {
                LocalDate start = LocalDate.parse(Objects.toString(msg.get("start")));
                LocalDate end   = LocalDate.parse(Objects.toString(msg.get("end")));
                List<EmployeeHoursSummary> rows = report.callSummarize(start, end);
                sendJson(s, Map.of("type","hours","start",start,"end",end,"rows",rows));
            }
            default -> sendJson(s, Map.of("type","error","message","unknown type: "+type));
        }
    }

    @Override public void afterConnectionClosed(WebSocketSession s, CloseStatus status) {
        sessionMap.remove(s.getId());
    }

    @Scheduled(fixedRate = 10_000)
    public void cleanUpStaleSessions() {
        Instant now = Instant.now();
        var it = sessionMap.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            var sess = e.getValue().session();
            var last = e.getValue().lastSeen();
            if (now.toEpochMilli() - last.toEpochMilli() > TIMEOUT_MILLIS) {
                try { if (sess.isOpen()) sess.close(CloseStatus.SESSION_NOT_RELIABLE); } catch (IOException ignored) {}
                it.remove();
            }
        }
    }

    public void broadcastHours(List<EmployeeHoursSummary> rows) {
        broadcastJson(Map.of("type","hours","rows", rows));
    }

    private void broadcastJson(Object obj) {
        String json;
        try { json = mapper.writeValueAsString(obj); } catch (Exception e) { return; }
        sessionMap.values().stream().map(Sess::session).filter(WebSocketSession::isOpen).forEach(s -> {
            try { s.sendMessage(new TextMessage(json)); } catch (IOException ignored) {}
        });
    }
    private void sendJson(WebSocketSession s, Object obj) throws Exception {
        s.sendMessage(new TextMessage(mapper.writeValueAsString(obj)));
    }
}
