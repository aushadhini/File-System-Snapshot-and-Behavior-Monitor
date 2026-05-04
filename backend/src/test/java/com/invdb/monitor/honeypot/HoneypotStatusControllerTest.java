package com.invdb.monitor.honeypot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class HoneypotStatusControllerTest {

    @Test
    void getStatusShouldReturnServiceResponse() {
        HoneypotStatusService statusService = mock(HoneypotStatusService.class);
        HoneypotStatus expected = new HoneypotStatus();
        expected.setEnabled(true);
        when(statusService.getStatus()).thenReturn(expected);

        HoneypotStatusController controller = new HoneypotStatusController(statusService);

        HoneypotStatus result = controller.getStatus();

        verify(statusService).getStatus();
        assertThat(result).isSameAs(expected);
    }
}
