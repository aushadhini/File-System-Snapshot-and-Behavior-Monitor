package com.invdb.monitor.event;

import com.invdb.monitor.risk.RiskLevel;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileEvent {
    private String path;
    private Instant timestamp;
    private FileEventType eventType;
    private boolean isHoneypotTriggered;
    private int riskScore;
    private RiskLevel riskLevel;
    private List<String> notes;
}
