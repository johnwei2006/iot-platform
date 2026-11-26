package com.iot.platform.service.uplink;

import com.iot.platform.mq.UplinkMessage;
import com.iot.platform.realtime.GatewayRealtimeStatusService;
import com.iot.platform.service.converter.UplinkBusinessDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SideHandlersTest {

    @Test
    public void businessConversionDelegatesToDispatcher() {
        UplinkBusinessDispatcher dispatcher = mock(UplinkBusinessDispatcher.class);
        UplinkMessage up = new UplinkMessage();

        new BusinessConversionHandler(provider(dispatcher)).handle(up);

        verify(dispatcher).dispatch(up);
    }

    @Test
    public void missingDispatcherIsNoOp() {
        new BusinessConversionHandler(provider(null)).handle(new UplinkMessage());
    }

    @Test
    public void businessConversionFailureIsNotSwallowedByHandler() {
        UplinkBusinessDispatcher dispatcher = mock(UplinkBusinessDispatcher.class);
        doThrow(new RuntimeException("writer down"))
                .when(dispatcher).dispatch(any(UplinkMessage.class));

        assertThrows(RuntimeException.class, () ->
                new BusinessConversionHandler(provider(dispatcher)).handle(new UplinkMessage()));
    }

    @Test
    public void realtimeStatusDelegatesToService() {
        GatewayRealtimeStatusService service = mock(GatewayRealtimeStatusService.class);
        UplinkMessage up = new UplinkMessage();

        new RealtimeStatusHandler(provider(service)).handle(up);

        verify(service).refresh(up);
    }

    @Test
    public void missingRealtimeStatusServiceIsNoOp() {
        new RealtimeStatusHandler(provider(null)).handle(new UplinkMessage());
    }

    @Test
    public void sideHandlersAreBestEffortAndNamed() {
        UplinkHandler conversion = new BusinessConversionHandler(provider(null));
        UplinkHandler realtime = new RealtimeStatusHandler(provider(null));

        assertFalse(conversion.critical());
        assertFalse(realtime.critical());
        assertEquals("businessConversion", conversion.name());
        assertEquals("realtimeStatus", realtime.name());
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
