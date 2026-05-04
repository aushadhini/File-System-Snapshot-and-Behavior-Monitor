package com.invdb.monitor.honeypot;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/honeypot")
public class HoneypotStatusController {

    private final HoneypotStatusService honeypotStatusService;

    public HoneypotStatusController(HoneypotStatusService honeypotStatusService) {
        this.honeypotStatusService = honeypotStatusService;
    }

    @GetMapping("/status")
    public HoneypotStatus getStatus() {
        return honeypotStatusService.getStatus();
    }
}
