package com.invdb.monitor.event;

import org.springframework.stereotype.Service;

@Service
public class InMemoryEventPublisher implements EventPublisher {

    private final EventPipelineService eventPipelineService;

    public InMemoryEventPublisher(EventPipelineService eventPipelineService) {
        this.eventPipelineService = eventPipelineService;
    }

    @Override
    public void publish(FileEvent event) {
        eventPipelineService.process(event);
    }
}
