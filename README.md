# Heartbeat & Website Uptime Monitoring Engine

-----------------------------------------------------------------------------------------------
### Submitted by: Vishal Gadade
### Email: vishalgadade18@gmail.com
### Phone: 9307005991

-----------------------------------------------------------------------------------------------

A resilient, event-driven backend service built with Java 17 and Spring Boot to monitor the health and execution of scheduled background jobs (heartbeats) and external websites.
The engine detects silent failures where expected events fail to happen. It evaluates deadlines autonomously via background schedulers, dispatches alerts through webhooks, and enforces strict operational guarantees around concurrency, non-blocking I/O, and query scalability.
------------------------------------------------------------------------------------------------

## 1. Stack & Versions

- **Language:** Java 17 (OpenJDK / Eclipse Temurin)
- **Framework:** Spring Boot 4.1.1 (Spring WebMVC, Spring Data JPA, Jakarta Validation, Jackson)
- **Build System:** Apache Maven (bundled via Maven Wrapper `mvnw` / `mvnw.cmd`)
- **Primary Database:** MySQL 8.0 (configured via `docker-compose.yml`)
- **Alternative / Test Database:** H2 In-Memory (zero-configuration local execution)

------------------------------------------------------------------------------------------------

## 2. What It Is & How to Run It (Clean Machine Setup)

### Prerequisites
- JDK 17 or higher (`java -version`).
- Docker Desktop (optional, only if running MySQL).
- **No manual Maven installation needed:** The included Maven Wrapper handles all dependencies automatically.

### Step 1: Install Dependencies & Compile
```bash
# Windows
.\mvnw.cmd clean package -DskipTests

# macOS / Linux
./mvnw clean package -DskipTests
```

### Step 2: Run the Automated Tests
```bash
# Windows
.\mvnw.cmd test

# macOS / Linux
./mvnw test
```

### Step 3: Start the Application

Choose **Option A** (Instant In-Memory H2, zero dependencies) or **Option B** (MySQL via Docker).

#### Option A: In-Memory Mode (Recommended for Instant Review)
No external database setup or Docker needed. Runs immediately using the embedded H2 profile:
```bash
# Windows
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=h2

# macOS / Linux
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

#### Option B: Production MySQL Mode (via Docker)
```bash
# 1. Start MySQL container
docker-compose up -d

# 2. Start application (Windows)
.\mvnw.cmd spring-boot:run

# 2. Start application (macOS / Linux)
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`.

-------------------------------------------------------------------------------------------------

## 3. Database Setup & Initialization

### Option 1: MySQL 8.0 via Docker Compose (Primary)
The included `docker-compose.yml` configures a local MySQL 8.0 container:
- **Port:** `3306`
- **Database:** `monitor_db`
- **Username / Password:** `root` / `root`
- **Start:** `docker-compose up -d`
- **Stop:** `docker-compose down`

Hibernate automatically creates and updates all schemas and indexes on application boot (`spring.jpa.hibernate.ddl-auto=update`).

### Option 2: Embedded H2 Database (Zero-Install Alternative)
Using `-Dspring-boot.run.profiles=h2` boots an in-memory MySQL-compatible H2 database with schema generated automatically.

-------------------------------------------------------------------------------------------------

## 4. How to Create a Check and Ping It

### Step A: Register a Heartbeat Check
```bash
curl -X POST http://localhost:8080/checks \
  -H "Content-Type: application/json" \
  -d '{
    "name": "nightly-db-backup",
    "kind": "heartbeat",
    "scheduleType": "interval",
    "scheduleValue": "60",
    "gracePeriodSeconds": 300,
    "alertUrl": "https://webhook.site/your-uuid"
  }'
```

**Response (HTTP 200 OK):**
```json
{
  "id": 1,
  "name": "nightly-db-backup",
  "kind": "HEARTBEAT",
  "status": "UP",
  "pingUrl": "http://localhost:8080/ping/6c9f28a2-a169-450a-9d33-4f93e3d4891b",
  "secretToken": "6c9f28a2-a169-450a-9d33-4f93e3d4891b",
  "scheduleType": "interval",
  "scheduleValue": "60",
  "gracePeriodSeconds": 300,
  "lastSeenAt": "2026-10-24T02:00:00",
  "nextExpectedAt": "2026-10-24T03:00:00",
  "alertUrl": "https://webhook.site/your-uuid"
}
```

### Step B: Ping the Check
Call the returned `pingUrl` (supports both `POST` and `GET`):
```bash
curl -X POST http://localhost:8080/ping/6c9f28a2-a169-450a-9d33-4f93e3d4891b
```

**Response (HTTP 200 OK):**
```text
Ping recorded successfully. Next expected at: 2026-10-24T04:00:00
```

---------------------------------------------------------------------------------------

## 5. Endpoints Reference & Examples

### 1. Register a Check (Heartbeat or Website)
- **Endpoint:** `POST /checks`
- **Example Heartbeat (Interval):**
  ```bash
  curl -X POST http://localhost:8080/checks \
    -H "Content-Type: application/json" \
    -d '{
      "name": "hourly-import",
      "kind": "heartbeat",
      "scheduleType": "interval",
      "scheduleValue": "60",
      "gracePeriodSeconds": 300,
      "alertUrl": "https://webhook.site/test"
    }'
  ```
- **Example Heartbeat (Unix 5-Field Cron):**
  ```bash
  curl -X POST http://localhost:8080/checks \
    -H "Content-Type: application/json" \
    -d '{
      "name": "weekday-payroll",
      "kind": "heartbeat",
      "scheduleType": "cron",
      "scheduleValue": "0 9 * * 1-5",
      "gracePeriodSeconds": 600,
      "alertUrl": "https://webhook.site/test"
    }'
  ```
  *(A ping on Friday morning 09:05 correctly schedules next expected deadline for Monday 09:00).*
- **Example Website Check:**
  ```bash
  curl -X POST http://localhost:8080/checks \
    -H "Content-Type: application/json" \
    -d '{
      "name": "corporate-portal",
      "kind": "website",
      "targetUrl": "https://api.github.com",
      "websiteIntervalSeconds": 60,
      "alertUrl": "https://webhook.site/test"
    }'
  ```

### 2. Ingest Heartbeat Ping
- **Endpoint:** `GET /ping/{token}` or `POST /ping/{token}`
- **Example:**
  ```bash
  curl -fsS http://localhost:8080/ping/6c9f28a2-a169-450a-9d33-4f93e3d4891b
  ```

### 3. Get Check Details (Masks Secret Token)
- **Endpoint:** `GET /checks/{id}`
- **Example:**
  ```bash
  curl http://localhost:8080/checks/1
  ```
  *(Rule 2: Never exposes private `secretToken` or `pingUrl`).*

### 4. List Checks (Paginated with Status Filter)
- **Endpoint:** `GET /checks?status={status}&page={page}&size={size}`
- **Example:**
  ```bash
  curl "http://localhost:8080/checks?status=DOWN&page=0&size=20"
  ```

### 5. Check History & Audit Trail
- **Endpoint:** `GET /checks/{id}/history?page={page}&size={size}`
- **Example:**
  ```bash
  curl "http://localhost:8080/checks/1/history?page=0&size=20"
  ```
  *(Returns chronological pings, website probe status codes, latency in ms, and status transitions).*

### 6. Pause Monitoring
- **Endpoint:** `POST /checks/{id}/pause`
- **Example:**
  ```bash
  curl -X POST http://localhost:8080/checks/1/pause
  ```
  *(Suspends sweeper evaluations and suppresses all alert dispatches).*

### 7. Resume Monitoring
- **Endpoint:** `POST /checks/{id}/resume`
- **Example:**
  ```bash
  curl -X POST http://localhost:8080/checks/1/resume
  ```
  *(Re-arms deadline starting from the current time; never fires alerts for missed time during pause).*

### 8. Dead-Letter Queue (DLQ) Audit
- **Endpoint:** `GET /alerts/failed?status=FAILED_PERMANENTLY`
- **Example:**
  ```bash
  curl "http://localhost:8080/alerts/failed?status=FAILED_PERMANENTLY"
  ```

-------------------------------------------------------------------------------------------------------------------------------

## 6. How the 6 Core Rules are Implemented

### Rule 1: One Alert Per Outage (Deduplication)
- State transitions follow `UP` -> `LATE` (grace period, no alert) -> `DOWN` (alert sent).
- The transition into `DOWN` persists an `Alert` record via the **Transactional Outbox Pattern**.
- The sweeper evaluates every 10 seconds. For prolonged outages (e.g. 1,080 sweeps over 3 hours), the check is recognized as already `DOWN`, suppressing duplicate notifications.
- When a ping or successful probe arrives while `DOWN`, exactly 1 recovery alert is dispatched before returning state to `UP`.

### Rule 2: Unpredictable Ping URLs
- A random UUID `secretToken` is generated on creation (`/ping/{token}`).
- Check IDs and names cannot be used to deduce the ping URL.
- Ordinary `GET /checks/{id}` detail queries mask the private secret token.

### Rule 3: Server Decides Time
- Client timestamps are completely ignored. The engine evaluates all deadlines against `LocalDateTime.now()` on the server clock.

### Rule 4: Concurrency & Ping vs Sweeper Race Condition
- **Problem:** At `12:00:10.000` sweeper marks check overdue, while at `12:00:10.001` a late ping arrives.
- **Solution:**
  - `Check` entity uses JPA Pessimistic Write Locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`).
  - All sweeper state-transition logic lives in `SweepTransitionService` (a separate `@Service`). This is critical: Spring's `@Transactional` is implemented via AOP proxy. If `SweeperService` called its own `@Transactional` methods internally (`this.method()`), the call would bypass the proxy and no transaction would ever open — causing `TransactionRequiredException` when the pessimistic lock is attempted. By extracting to a separate bean, all calls go through the Spring proxy and the transaction is guaranteed to start.
  - Ping updates run through `PingTxService` under `PESSIMISTIC_WRITE` lock in the same transaction. If the sweeper already owns the row lock, the ping waits and reads the committed state after the lock is released — preventing stale-read races.

### Rule 5: Scalability with 10,000 Checks (O(log N) Sweeper)
- The sweeper **never** calls `findAll()` (which would cause 86 million reads a day).
- Four narrow index-range queries use the composite index `(kind, status, next_expected_at)`:
  - `(HEARTBEAT, UP, <= now)` · `(HEARTBEAT, LATE, <= now)` · `(WEBSITE, UP, <= now)` · `(WEBSITE, DOWN, <= now)`
- Each query fetches up to **2,000 rows** per sweep tick. At 10,000 website checks on a 60-second interval, approximately 167 checks become due per 10-second sweep — well within a single page. Raise `SWEEP_PAGE_SIZE` in `CheckRepository` if check density grows further.

### Rule 6: Slow Websites Do Not Block Sweeping (Thread Isolation)
- Website probing uses the JDK non-blocking `HttpClient.sendAsync()` — zero blocking threads per probe.
- Each probe has a **hard 5-second deadline**: `HttpRequest.timeout(5s)` covers the response headers; `.orTimeout(5, SECONDS)` on the `CompletableFuture` covers the full round-trip (headers + body read). A server that drips a body past 5 seconds is cancelled and marked DOWN.
- Body is read with `BodyHandlers.discarding()` — response body is never accumulated in memory.
- 100 simultaneous hanging endpoints all expire together in ≈5 seconds without ever blocking the sweeper loop.

### Resilient Webhook Delivery & Dead-Letter Queue (DLQ)
- Alerts are dispatched asynchronously via non-blocking `HttpClient.sendAsync()`.
- Broken receiver URLs do not block other alerts.
- Retries follow **3× exponential backoff**: 5 min → 15 min → 45 min → 2h15m → 6h45m (≈11 h total window), covering the "dead for six hours" example from the assignment.
- Exhausted retries (5 attempts) transition to `FAILED_PERMANENTLY` and are preserved for audit via `GET /alerts/failed`.
- Permanently failed alerts can be re-queued for immediate re-delivery: `POST /alerts/failed/{id}/retry`.

------------------------------------------------------------------------------------------------------------------------------

## 7. Architecture & Package Structure

Organized cleanly by **Package-by-Feature (Domain-Driven Design)**:

```
src/main/java/com/monitor/heartbeat_monitor/
├── check/                            # Check CRUD, status, pause/resume
│   ├── controller/CheckController.java
│   ├── service/CheckService.java
│   ├── repository/CheckRepository.java
│   ├── model/Check.java              # Entity with @Version optimistic lock
│   ├── dto/                          # CreateCheckRequest, CreateCheckResponse, CheckDetailsResponse
│   └── enums/                        # CheckStatus (UP, LATE, DOWN, PAUSED), CheckKind
├── ping/                             # Fast ping ingestion
│   ├── controller/PingController.java
│   └── service/PingTxService.java    # Isolated REQUIRES_NEW transaction + collision retry
├── schedule/                         # Scheduling engine
│   ├── ScheduleCalculator.java       # Interface
│   └── impl/DefaultScheduleCalculator.java # 5-field UNIX Cron & Interval computation
├── sweeper/                          # Sweeper scheduler (runs every 10s)
│   ├── scheduler/Sweeper.java        # @Scheduled(fixedDelay = 10000)
│   ├── service/SweeperService.java   # Loop orchestration — no @Transactional (avoids self-invocation proxy bypass)
│   └── service/SweepTransitionService.java  # All @Transactional state transitions (UP→LATE→DOWN, website result handling)
├── webcheck/                         # Non-blocking website probing (5s timeout)
│   ├── service/WebCheckExecutor.java
│   ├── constant/ProbeMessages.java
│   └── dto/ProbeResult.java
├── alert/                            # Transactional Outbox pattern
│   ├── model/Alert.java
│   ├── repository/AlertRepository.java
│   ├── service/AlertService.java
│   ├── constant/AlertNotificationMessages.java
│   └── enums/AlertStatus.java        # PENDING, DELIVERED, FAILED_PERMANENTLY
├── delivery/                         # Webhook delivery & retry pipeline
│   ├── worker/AlertDeliveryWorker.java # @Scheduled(fixedDelay = 15000) batch retry
│   ├── policy/BackoffPolicy.java     # Exponential backoff calculator
│   └── controller/AlertDeliveryController.java # Dead-Letter Queue inspection
├── history/                          # Audit trail
│   ├── model/CheckHistory.java
│   ├── repository/CheckHistoryRepository.java
│   ├── constant/HistoryMessages.java
│   └── enums/HistoryEventType.java   # REGISTERED, PING, WEBSITE_PROBE, STATUS_CHANGE
└── common/                           # Cross-cutting infrastructure
    ├── config/                       # AppConfig, ThreadPoolConfig
    ├── constant/LogMessages.java     # Centralized logging templates
    └── exception/                    # GlobalExceptionHandler, ResourceNotFoundException
```


-----------------------------------------------------------------------------------------------------------------------------

## 8. Assumptions & Trade-offs

### Assumptions Made
1. **Interval unit is seconds** for `scheduleValue` when `scheduleType=interval`. All timing is server-side.
2. **Cron early-ping window is 5 minutes.** When a ping arrives and the stored deadline is still in the future but within 5 minutes, the next deadline is calculated from the stored slot — so an early ping at 02:59:50 for a 03:00 job correctly advances to tomorrow 03:00 with no false LATE alarm. Outside that window (stale deadline from a past outage, or repeated rapid pings), the calculation starts from `now` — preventing a stale-deadline from producing a past-due next slot and preventing deadline ratcheting from multiple rapid pings.
3. **Grace period 0 is valid** — a check with `gracePeriodSeconds=0` goes directly from UP to DOWN with no LATE phase.
4. **Website checks have no secretToken or pingUrl** — they are probed by the sweeper automatically.
5. **Webhook URLs are trusted** — no outbound allowlist; any valid http(s) URL is accepted.

### Known Limitations (not implemented in this submission)
- **History retention**: Every website probe creates a history row. At 10,000 checks on 60-second intervals this is ~14 million rows/day. A scheduled purge (e.g. DELETE WHERE createdAt < NOW() - 30 DAYS) or partitioning is recommended for production but is out of scope here.
- **Sweeper backpressure at extreme scale**: Each of the four index-range queries is capped at 2,000 rows. In the extreme case where more than 2,000 checks of the same kind/status become due simultaneously, excess rows are deferred to the next sweep (10 s). For this assignment's 10,000-check scenario this never occurs.
- **Cron timezone**: Cron expressions use the server's system timezone. A future enhancement could accept a per-check timezone.
- **No idempotency key on pings**: A network retry could create a duplicate PING history entry. The status transitions are idempotent (UP stays UP), but the history row count may differ.
