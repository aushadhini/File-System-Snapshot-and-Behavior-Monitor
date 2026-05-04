package com.invdb.monitor.risk;

import com.invdb.monitor.event.FileEvent;

public interface RiskEngine {

    RiskAssessment calculateRisk(FileEvent event);
}
