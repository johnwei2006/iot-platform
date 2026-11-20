package com.iot.platform.service;

import com.iot.platform.mq.DownlinkMessage;
import com.iot.platform.service.downlink.DownlinkHandler;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DownlinkServiceTest {

    @Test
    public void executesHandlersInOrder() {
        DownlinkHandler first = handler("first");
        DownlinkHandler second = handler("second");
        DownlinkMessage dm = new DownlinkMessage();

        new DownlinkService(Arrays.asList(first, second)).send(dm);

        InOrder order = inOrder(first, second);
        order.verify(first).handle(dm);
        order.verify(second).handle(dm);
    }

    @Test
    public void failurePropagatesAndStopsChain() {
        DownlinkHandler first = handler("first");
        doThrow(new RuntimeException("validation failed"))
                .when(first).handle(any(DownlinkMessage.class));
        DownlinkHandler second = handler("second");

        try {
            new DownlinkService(Arrays.asList(first, second)).send(new DownlinkMessage());
            fail("downlink failure should propagate");
        } catch (RuntimeException e) {
            assertEquals("validation failed", e.getMessage());
        }
        verify(second, never()).handle(any(DownlinkMessage.class));
    }

    @Test
    public void emptyHandlerListIsNoOp() {
        new DownlinkService(Collections.<DownlinkHandler>emptyList()).send(new DownlinkMessage());
    }

    private DownlinkHandler handler(String name) {
        DownlinkHandler handler = mock(DownlinkHandler.class);
        when(handler.name()).thenReturn(name);
        return handler;
    }
}
