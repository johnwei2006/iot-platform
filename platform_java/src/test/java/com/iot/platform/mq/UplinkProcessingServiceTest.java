package com.iot.platform.mq;

import com.iot.platform.service.uplink.UplinkHandler;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UplinkProcessingServiceTest {

    @Test
    public void executesHandlersInOrder() {
        UplinkHandler first = handler("first", false);
        UplinkHandler second = handler("second", false);
        UplinkProcessingService service = new UplinkProcessingService(Arrays.asList(first, second));
        UplinkMessage up = new UplinkMessage();

        service.process(up);

        InOrder order = inOrder(first, second);
        order.verify(first).handle(up);
        order.verify(second).handle(up);
    }

    @Test
    public void criticalFailurePropagatesAndStopsChain() {
        UplinkHandler critical = handler("critical", true);
        doThrow(new RuntimeException("db down")).when(critical).handle(any(UplinkMessage.class));
        UplinkHandler next = handler("next", false);
        UplinkProcessingService service =
                new UplinkProcessingService(Arrays.asList(critical, next));

        try {
            service.process(new UplinkMessage());
            fail("critical failure should propagate");
        } catch (RuntimeException e) {
            assertEquals("db down", e.getMessage());
        }
        verify(next, never()).handle(any(UplinkMessage.class));
    }

    @Test
    public void sideFailureIsSwallowedAndChainContinues() {
        UplinkHandler side = handler("side", false);
        doThrow(new IllegalStateException("redis down")).when(side).handle(any(UplinkMessage.class));
        UplinkHandler next = handler("next", true);
        UplinkProcessingService service = new UplinkProcessingService(Arrays.asList(side, next));

        service.process(new UplinkMessage());

        verify(next).handle(any(UplinkMessage.class));
    }

    @Test
    public void emptyHandlerListIsNoOp() {
        new UplinkProcessingService(Collections.<UplinkHandler>emptyList())
                .process(new UplinkMessage());
    }

    private UplinkHandler handler(String name, boolean critical) {
        UplinkHandler handler = mock(UplinkHandler.class);
        when(handler.name()).thenReturn(name);
        when(handler.critical()).thenReturn(critical);
        return handler;
    }
}
