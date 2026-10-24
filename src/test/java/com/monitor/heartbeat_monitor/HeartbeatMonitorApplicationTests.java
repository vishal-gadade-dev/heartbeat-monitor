package com.monitor.heartbeat_monitor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class HeartbeatMonitorApplicationTests {

	@Test
	void contextLoads() {
	}

}
