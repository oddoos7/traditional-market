package com.traditionalmarket.backend.common;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.traditionalmarket.backend.common.logging.RequestLoggingFilter;

@SpringBootTest
@ActiveProfiles("local")
class OperationalQualityIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RequestLoggingFilter requestLoggingFilter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .addFilters(requestLoggingFilter)
                .build();
    }

    @Test
    void exposesOpenApiDocumentWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("전통시장 API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.paths['/api/markets']").exists());
    }

    @Test
    void preservesSafeClientRequestId() throws Exception {
        mockMvc.perform(get("/api/health")
                        .header(RequestLoggingFilter.REQUEST_ID_HEADER, "quality-test-1234"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        RequestLoggingFilter.REQUEST_ID_HEADER,
                        "quality-test-1234"
                ));
    }

    @Test
    void replacesUnsafeClientRequestId() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/health")
                        .header(RequestLoggingFilter.REQUEST_ID_HEADER, "unsafe request id"))
                .andExpect(status().isOk())
                .andReturn();

        String generatedRequestId = result.getResponse()
                .getHeader(RequestLoggingFilter.REQUEST_ID_HEADER);

        assertNotNull(generatedRequestId);
        assertNotEquals("unsafe request id", generatedRequestId);
        assertTrue(generatedRequestId.matches("[0-9a-f-]{36}"));
    }
}
