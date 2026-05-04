package com.invdb.monitor.risk;

import com.invdb.monitor.event.FileEvent;
import org.springframework.stereotype.Service;

@Service
public class RiskEngineImpl implements RiskEngine {

    private static final String MASS_CHANGE_SUSPECTED = "MASS_CHANGE_SUSPECTED";
    private static final String RAPID_DELETE_SPIKE = "RAPID_DELETE_SPIKE";
    private static final String SUSPICIOUS_EXTENSION = "SUSPICIOUS_EXTENSION";
    private static final String CRITICAL_INTRUSION_PATTERN = "CRITICAL_INTRUSION_PATTERN";

    @Override
    public RiskAssessment calculateRisk(FileEvent event) {
        if (event.getNotes() != null && event.getNotes().contains(CRITICAL_INTRUSION_PATTERN)) {
            return new RiskAssessment(100, RiskLevel.HIGH);
        }

        int score = switch (event.getEventType()) {
            case CREATED -> 15;
            case MODIFIED -> 25;
            case DELETED -> 20;
        };

        if (event.isHoneypotTriggered()) {
            score = 95;
        }

        if (event.getNotes() != null && event.getNotes().contains(MASS_CHANGE_SUSPECTED)) {
            score += 20;
        }

        if (event.getNotes() != null && event.getNotes().contains(RAPID_DELETE_SPIKE)) {
            score += 25;
        }

        if (event.getNotes() != null && event.getNotes().contains(SUSPICIOUS_EXTENSION)) {
            score += 30;
        }

        int cappedScore = Math.min(100, score);
        return new RiskAssessment(cappedScore, toRiskLevel(cappedScore));
    }

    private RiskLevel toRiskLevel(int score) {
        if (score <= 30) {
            return RiskLevel.LOW;
        }
        if (score <= 70) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.HIGH;
    }
}
