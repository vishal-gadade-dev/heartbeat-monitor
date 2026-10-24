package com.monitor.heartbeat_monitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitor.heartbeat_monitor.alert.enums.AlertStatus;
import com.monitor.heartbeat_monitor.alert.model.Alert;
import com.monitor.heartbeat_monitor.alert.repository.AlertRepository;
import com.monitor.heartbeat_monitor.check.dto.CreateCheckRequest;
import com.monitor.heartbeat_monitor.check.enums.CheckKind;
import com.monitor.heartbeat_monitor.check.enums.CheckStatus;
import com.monitor.heartbeat_monitor.check.model.Check;
import com.monitor.heartbeat_monitor.check.repository.CheckRepository;
import com.monitor.heartbeat_monitor.history.enums.HistoryEventType;
import com.monitor.heartbeat_monitor.history.repository.CheckHistoryRepository;
import com.monitor.heartbeat_monitor.sweeper.service.SweepTransitionService;
import com.monitor.heartbeat_monitor.sweeper.service.SweeperService;
import com.monitor.heartbeat_monitor.webcheck.dto.ProbeResult;
import com.monitor.heartbeat_monitor.webcheck.service.WebCheckExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class WebsiteAndAlertIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CheckRepository checkRepository;

    @Autowired
    private CheckHistoryRepository historyRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private SweeperService sweeperService;

    @Autowired
    private SweepTransitionService transitionService;

    /** Prevents any test from making real outbound HTTP calls. */
    @MockitoBean
    private WebCheckExecutor webCheckExecutor;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        alertRepository.deleteAll();
        historyRepository.deleteAll();
        checkRepository.deleteAll();

        // Default probe response: UP — tests that need DOWN configure it themselves
        when(webCheckExecutor.probeAsync(anyString()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(
                        ProbeResult.builder().up(true).statusCode(200).responseTimeMs(50L).message("HTTP 200 OK").build()
                ));
    }

    @Test
    void testRegisterWebsiteCheck_AndGetSingleStatus() throws Exception {
        CreateCheckRequest request = CreateCheckRequest.builder()
                .name("main-marketing-site")
                .kind("website")
                .targetUrl("https://example.com")
                .websiteIntervalSeconds(60)
                .alertUrl("https://alerts.example.com/webhook")
                .build();

        String responseJson = mockMvc.perform(post("/checks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("main-marketing-site"))
                .andExpect(jsonPath("$.kind").value("WEBSITE"))
                .andExpect(jsonPath("$.targetUrl").value("https://example.com"))
                .andReturn().getResponse().getContentAsString();

        Long checkId = objectMapper.readTree(responseJson).get("id").asLong();

        mockMvc.perform(get("/checks/" + checkId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(checkId))
                .andExpect(jsonPath("$.name").value("main-marketing-site"))
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/checks/" + checkId + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventType").value("REGISTERED"));
    }

    @Test
    void testWebsiteProbeResult_DownAndRecoveryHandling() {
        Check check = checkRepository.save(Check.builder()
                .name("api-service")
                .kind(CheckKind.WEBSITE)
                .targetUrl("https://api.example.com")
                .websiteIntervalSeconds(120)
                .status(CheckStatus.UP)
                .alertUrl("https://alerts.example.com")
                .build());

        LocalDateTime now = LocalDateTime.now();

        ProbeResult failedResult = ProbeResult.builder()
                .up(false)
                .statusCode(500)
                .responseTimeMs(450)
                .message("HTTP error: 500")
                .build();

        transitionService.handleWebsiteProbeResult(check.getId(), failedResult, now, 120);

        Check downCheck = checkRepository.findById(check.getId()).orElseThrow();
        assertEquals(CheckStatus.DOWN, downCheck.getStatus());

        ProbeResult recoveredResult = ProbeResult.builder()
                .up(true)
                .statusCode(200)
                .responseTimeMs(85)
                .message("HTTP 200 OK")
                .build();

        transitionService.handleWebsiteProbeResult(check.getId(), recoveredResult, now.plusMinutes(2), 120);

        Check recoveredCheck = checkRepository.findById(check.getId()).orElseThrow();
        assertEquals(CheckStatus.UP, recoveredCheck.getStatus());
        assertNotNull(recoveredCheck.getLastSeenAt());
    }

    @Test
    void testDeadLetterAlertsEndpoint() throws Exception {
        alertRepository.save(Alert.builder()
                .checkId(99L)
                .checkName("dead-job")
                .alertUrl("https://dead-endpoint.com")
                .eventType("DOWN")
                .payload("{\"status\":\"DOWN\"}")
                .retryCount(5)
                .status(AlertStatus.FAILED_PERMANENTLY)
                .lastError("Connect timed out")
                .build());

        mockMvc.perform(get("/alerts/failed?status=FAILED_PERMANENTLY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].checkName").value("dead-job"))
                .andExpect(jsonPath("$.content[0].status").value("FAILED_PERMANENTLY"));

        mockMvc.perform(get("/alerts/failed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("FAILED_PERMANENTLY"));
    }
}
