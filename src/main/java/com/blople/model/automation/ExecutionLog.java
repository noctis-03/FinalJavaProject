package com.blople.model.automation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 자동화 실행 이력 (FR-12). execution_log 테이블과 매핑.
 */
public class ExecutionLog {

    public static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED  = "FAILED";

    private String id;
    private String ruleId;
    private String executedAt;
    private String status;        // SUCCESS / FAILED
    private String resultMessage;

    public ExecutionLog(String ruleId, String status, String resultMessage) {
        this.id            = UUID.randomUUID().toString();
        this.ruleId        = ruleId;
        this.executedAt    = LocalDateTime.now().format(TS_FMT);
        this.status        = status;
        this.resultMessage = resultMessage;
    }

    public static ExecutionLog reconstruct(String id, String ruleId, String executedAt,
                                           String status, String resultMessage) {
        ExecutionLog l = new ExecutionLog(ruleId, status, resultMessage);
        l.id         = id;
        l.executedAt = executedAt;
        return l;
    }

    public String getId()            { return id; }
    public String getRuleId()        { return ruleId; }
    public String getExecutedAt()    { return executedAt; }
    public String getStatus()        { return status; }
    public String getResultMessage() { return resultMessage; }

    public boolean isSuccess() { return STATUS_SUCCESS.equals(status); }
}
