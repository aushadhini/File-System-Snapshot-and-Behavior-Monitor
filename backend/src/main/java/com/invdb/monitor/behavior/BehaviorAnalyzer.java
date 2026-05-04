package com.invdb.monitor.behavior;

import com.invdb.monitor.event.FileEvent;

public interface BehaviorAnalyzer {

    void analyze(FileEvent event);
}
