package com.invdb.monitor.event;

public interface EventPublisher {

    void publish(FileEvent event);
}
