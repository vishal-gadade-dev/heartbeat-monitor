package com.monitor.heartbeat_monitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitor.heartbeat_monitor.alert.model.Alert;
import com.monitor.heartbeat_monitor.alert.repository.AlertRepository;
import com.monitor.heartbeat_monitor.check.dto.CreateCheckRequest;
import com.monitor.heartbeat_monitor.check.enums.CheckKind;
import com.monitor.heartbeat_monitor.check.enums.CheckStatus;
import com.monitor.heartbeat_monitor.check.model.Check;
import com.monitor.heartbeat_monitor.check.repository.CheckRepository;
import com.monitor.heartbeat_monitor.history.enums.HistoryEventType;
import com.monitor.heartbeat_monitor.history.model.CheckHistory;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class HeartbeatMonitorIntegrationTest {

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
        when(webCheckExecutor.probeAsync(anyString()))
                .thenReturn(CompletableFuture.completedFuture(
                        ProbeResult.builder()
                                .up(true)
                                .statusCode(200)
                                .responseTimeMs(50L)
                                .message("HTTP 200 OK")
                                .build()
                ));
    }

    @Test
    void testRegisterHeartbeatCheck_ReturnsPrivatePingUrl() throws Exception {
        CreateCheckRequest request = CreateCheckRequest.builder()
                .name("hourly-backup")
                .kind("heartbeat")
                .scheduleType("interval")
                .scheduleValue("60")
                .gracePeriodSeconds(300)
                .alertUrl("https://example.com/alerts/webhook")
                .build();

        mockMvc.perform(post("/checks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("hourly-backup"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.pingUrl").exists())
                .andExpect(jsonPath("$.secretToken").exists());

        List<Check> checks = checkRepository.findAll();
        assertEquals(1, checks.size());
        assertEquals("hourly-backup", checks.get(0).getName());
        assertNotNull(checks.get(0).getSecretToken());
    }

    @Test
    void testPingEndpoint_UpdatesLastSeenAndRecordsHistory() throws Exception {
        Check check = checkRepository.save(Check.builder()
                .name("db-backup")
                .kind(CheckKind.HEARTBEAT)
                .secretToken("secret-token-1234")
                .scheduleType("interval")
                .scheduleValue("60")
                .gracePeriodSeconds(300)
                .status(CheckStatus.UP)
                .alertUrl("https://example.com/alerts")
                .build());

        mockMvc.perform(get("/ping/" + check.getSecretToken()))
                .andExpect(status().isOk());

        Check updated = checkRepository.findById(check.getId()).orElseThrow();
        assertNotNull(updated.getLastSeenAt());
        assertNotNull(updated.getNextExpectedAt());
        assertEquals(CheckStatus.UP, updated.getStatus());

        List<CheckHistory> history = historyRepository.findByCheckIdOrderByCreatedAtDesc(check.getId());
        assertEquals(1, history.size());
        assertEquals(HistoryEventType.PING, history.get(0).getEventType());
    }

    @Test
    void testSweeperTransitions_UpToLateToDown_AndRecoveryPingAlert() {
        LocalDateTime now = LocalDateTime.now();

        Check check = checkRepository.save(Check.builder()
                .name("nightly-import")
                .kind(CheckKind.HEARTBEAT)
                .secretToken("token-late-test")
                .scheduleType("interval")
                .scheduleValue("60")
                .gracePeriodSeconds(300)
                .nextExpectedAt(now.minusMinutes(2))
                .status(CheckStatus.UP)
                .alertUrl("https://example.com/alerts")
                .build());

        transitionService.processHeartbeatCheck(check, now);

        Check lateCheck = checkRepository.findById(check.getId()).orElseThrow();
        assertEquals(CheckStatus.LATE, lateCheck.getStatus());

        LocalDateTime pastGraceTime = now.plusMinutes(10);
        transitionService.processHeartbeatCheck(lateCheck, pastGraceTime);

        Check downCheck = checkRepository.findById(check.getId()).orElseThrow();
        assertEquals(CheckStatus.DOWN, downCheck.getStatus());

        try {
            mockMvc.perform(post("/ping/" + downCheck.getSecretToken()))
                    .andExpect(status().isOk());
        } catch (Exception e) {
            fail(e);
        }

        Check recoveredCheck = checkRepository.findById(check.getId()).orElseThrow();
        assertEquals(CheckStatus.UP, recoveredCheck.getStatus());
    }

    @Test
    void testSweepChecks_EndToEnd_LateTransition() {
        LocalDateTime now = LocalDateTime.now();

        // Deadline 2 minutes ago, grace 5 min → check is LATE (not yet DOWN)
        checkRepository.save(Check.builder()
                .name("overdue-job-late")
                .kind(CheckKind.HEARTBEAT)
                .secretToken("token-sweep-late")
                .scheduleType("interval")
                .scheduleValue("60")
                .gracePeriodSeconds(300)
                .nextExpectedAt(now.minusMinutes(2))
                .status(CheckStatus.UP)
                .alertUrl("https://example.com/alerts")
                .build());

        sweeperService.sweepChecks();

        Check result = checkRepository.findAll().stream()
                .filter(c -> "overdue-job-late".equals(c.getName()))
                .findFirst().orElseThrow();

        assertEquals(CheckStatus.LATE, result.getStatus(),
                "sweepChecks() must transition UP → LATE when deadline passed but still in grace period");

        List<Alert> alerts = alertRepository.findAll();
        assertEquals(0, alerts.size(), "No alert should be created for LATE transition");
    }

    @Test
    void testSweepChecks_EndToEnd_DownTransitionWithExactlyOneAlert() {
        LocalDateTime now = LocalDateTime.now();

        checkRepository.save(Check.builder()
                .name("overdue-job-down")
                .kind(CheckKind.HEARTBEAT)
                .secretToken("token-sweep-down")
                .scheduleType("interval")
                .scheduleValue("60")
                .gracePeriodSeconds(60)
                .nextExpectedAt(now.minusMinutes(5))
                .status(CheckStatus.LATE)
                .alertUrl("https://example.com/alerts")
                .build());

        sweeperService.sweepChecks();

        Check result = checkRepository.findAll().stream()
                .filter(c -> "overdue-job-down".equals(c.getName()))
                .findFirst().orElseThrow();

        assertEquals(CheckStatus.DOWN, result.getStatus(),
                "sweepChecks() must transition LATE → DOWN when grace period expired");

        List<Alert> alerts = alertRepository.findAll();
        assertEquals(1, alerts.size(),
                "Exactly one alert must be created for the DOWN transition");
        assertEquals("DOWN", alerts.get(0).getEventType(),
                "Alert eventType must be DOWN");
    }

    @Test
    void testPauseAndResume() throws Exception {
        Check check = checkRepository.save(Check.builder()
                .name("paused-job")
                .kind(CheckKind.HEARTBEAT)
                .secretToken("token-pause")
                .scheduleType("interval")
                .scheduleValue("60")
                .gracePeriodSeconds(300)
                .status(CheckStatus.UP)
                .nextExpectedAt(LocalDateTime.now().minusHours(2))
                .alertUrl("https://example.com/alerts")
                .build());

        mockMvc.perform(post("/checks/" + check.getId() + "/pause"))
                .andExpect(status().isOk());

        Check paused = checkRepository.findById(check.getId()).orElseThrow();
        assertEquals(CheckStatus.PAUSED, paused.getStatus());

        // Sweeper must ignore the PAUSED check even though it is overdue
        sweeperService.sweepChecks();

        Check afterSweep = checkRepository.findById(check.getId()).orElseThrow();
        assertEquals(CheckStatus.PAUSED, afterSweep.getStatus(),
                "Sweeper must not touch a PAUSED check");

        mockMvc.perform(post("/checks/" + check.getId() + "/resume"))
                .andExpect(status().isOk());

        Check resumed = checkRepository.findById(check.getId()).orElseThrow();
        assertEquals(CheckStatus.UP, resumed.getStatus());
        assertTrue(resumed.getNextExpectedAt().isAfter(LocalDateTime.now().plusMinutes(50)));
    }

    @Test
    void testListChecksPaginationAndStatusFilter() throws Exception {
        checkRepository.save(Check.builder().name("c1").kind(CheckKind.HEARTBEAT).alertUrl("u1").status(CheckStatus.UP).build());
        checkRepository.save(Check.builder().name("c2").kind(CheckKind.HEARTBEAT).alertUrl("u2").status(CheckStatus.DOWN).build());
        checkRepository.save(Check.builder().name("c3").kind(CheckKind.HEARTBEAT).alertUrl("u3").status(CheckStatus.DOWN).build());

        mockMvc.perform(get("/checks?status=DOWN&page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].status").value("DOWN"))
                .andExpect(jsonPath("$.content[1].status").value("DOWN"));
    }
}
