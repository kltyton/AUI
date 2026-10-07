package io.github.kltyton.kltytonui.render;

import io.github.kltyton.kltytonui.spi.KuiRenderService;
import io.github.kltyton.kltytonui.spi.KuiServices;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaseCommitDrawsTest {
    @Test
    void flushesMinecraftFontBuffersBeforeChangingRenderState() {
        AtomicInteger sharedFlushes = new AtomicInteger();
        KuiRenderService previous = KuiServices.render();
        KuiRenderService recording = (KuiRenderService) Proxy.newProxyInstance(
                KuiRenderService.class.getClassLoader(),
                new Class<?>[]{KuiRenderService.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("flushSharedBuffers")) {
                        sharedFlushes.incrementAndGet();
                    }
                    Class<?> type = method.getReturnType();
                    if (type == boolean.class) return false;
                    if (type == int.class) return 0;
                    if (type == float.class) return 0.0f;
                    return null;
                }
        );

        KuiServices.setRender(recording);
        try {
            Base.commitDraws();
            assertEquals(1, sharedFlushes.get());
        } finally {
            KuiServices.setRender(previous);
        }
    }
}
