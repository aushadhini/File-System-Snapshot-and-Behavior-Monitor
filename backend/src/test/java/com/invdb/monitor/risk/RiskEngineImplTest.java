package com.invdb.monitor.risk;

import static org.assertj.core.api.Assertions.assertThat;

import com.invdb.monitor.event.FileEvent;
import com.invdb.monitor.event.FileEventType;
import java.util.List;
import org.junit.jupiter.api.Test;

class RiskEngineImplTest {

    @Test
    void shouldAddConfiguredNoteBonusesAndCapAtOneHundred() {
        RiskEngineImpl riskEngine = new RiskEngineImpl();
        FileEvent event = FileEvent.builder()
                .eventType(FileEventType.MODIFIED)
                .notes(List.of("MASS_CHANGE_SUSPECTED", "RAPID_DELETE_SPIKE", "SUSPICIOUS_EXTENSION"))
                .build();

        RiskAssessment riskAssessment = riskEngine.calculateRisk(event);

        assertThat(riskAssessment.score()).isEqualTo(100);
        assertThat(riskAssessment.level()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void shouldForceHighRiskWhenCriticalIntrusionPatternExists() {
        RiskEngineImpl riskEngine = new RiskEngineImpl();
        FileEvent event = FileEvent.builder()
                .eventType(FileEventType.CREATED)
                .isHoneypotTriggered(false)
                .notes(List.of("CRITICAL_INTRUSION_PATTERN"))
                .build();

        RiskAssessment riskAssessment = riskEngine.calculateRisk(event);

        assertThat(riskAssessment.score()).isEqualTo(100);
        assertThat(riskAssessment.level()).isEqualTo(RiskLevel.HIGH);
    }

}
