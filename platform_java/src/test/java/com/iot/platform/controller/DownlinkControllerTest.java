package com.iot.platform.controller;

import com.iot.platform.mq.DownlinkMessage;
import com.iot.platform.security.JwtFilter;
import com.iot.platform.service.DownlinkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(
        controllers = DownlinkController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtFilter.class))
public class DownlinkControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private DownlinkService downlinkService;

    @Test
    public void postsDownlinkWithDeviceIdFromPath() throws Exception {
        mvc.perform(post("/api/devices/D1/downlink")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceType\":101,\"serviceData\":\"AQI=\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<DownlinkMessage> captor = ArgumentCaptor.forClass(DownlinkMessage.class);
        verify(downlinkService).send(captor.capture());
        assertEquals("D1", captor.getValue().getDeviceId());
        assertEquals(101, captor.getValue().getServiceType());
        assertEquals("AQI=", captor.getValue().getServiceData());
    }
}
