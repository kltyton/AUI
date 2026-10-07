package io.github.kltyton.kltytonui.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KuiLoggingTest {
    @Test
    void derivesKuiPathsBesideLatestLog() {
        assertEquals("logs/kui.log", KuiLogging.kuiFileName("logs/latest.log"));
        assertEquals("logs\\kui.log", KuiLogging.kuiFileName("logs\\latest.log"));
    }
}
