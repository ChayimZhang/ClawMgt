# ClawMgt Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the backend-only ClawMgt service from scratch, implementing channel-scoped tasks, chat tasks, Skill lifecycle tasks, node reports, and typed payload strategies.

**Architecture:** Spring Boot exposes management APIs and edge Claw APIs. Tasks are channel-scoped parent records with node-scoped task items, typed persisted payloads, task-item details, and task events. Task and report behavior is selected through strongly typed payloads and strategy registries; concurrency is controlled in code and `application.yml`, not stored in task tables.

**Tech Stack:** Java 21, Spring Boot 3.3.x, Spring Web, Spring Validation, Spring Data JPA, Flyway, H2, PostgreSQL-compatible SQL, Maven, JUnit 5.

---

## File Structure

Create this Maven project structure:

- `pom.xml`: Maven dependencies and Spring Boot plugin.
- `src/main/java/com/qwenpaw/clawmgt/ClawMgtApplication.java`: Spring Boot entrypoint.
- `src/main/resources/application.yml`: local H2 config and task concurrency config.
- `src/main/resources/db/migration/V1__init_schema.sql`: full schema from the approved design.
- `src/main/java/com/qwenpaw/clawmgt/common/ApiResponse.java`: consistent response wrapper.
- `src/main/java/com/qwenpaw/clawmgt/common/BusinessException.java`: domain exception.
- `src/main/java/com/qwenpaw/clawmgt/common/GlobalExceptionHandler.java`: validation and business error handling.
- `src/main/java/com/qwenpaw/clawmgt/domain/enums/*.java`: task, report, status, category, role, source, node enums.
- `src/main/java/com/qwenpaw/clawmgt/domain/entity/*.java`: JPA entities for channel, node, task, task item, task detail, event, session, message, report, and Skill metadata.
- `src/main/java/com/qwenpaw/clawmgt/domain/repository/*.java`: Spring Data repositories.
- `src/main/java/com/qwenpaw/clawmgt/api/dto/**/*.java`: strongly typed requests and responses.
- `src/main/java/com/qwenpaw/clawmgt/api/payload/task/*.java`: `TaskPayload` interface and concrete task payloads.
- `src/main/java/com/qwenpaw/clawmgt/api/payload/report/*.java`: `ReportPayload` interface and Skill metadata report payload.
- `src/main/java/com/qwenpaw/clawmgt/config/JacksonConfig.java`: task and report payload polymorphic binding.
- `src/main/java/com/qwenpaw/clawmgt/config/TaskConcurrencyProperties.java`: typed `application.yml` binding.
- `src/main/java/com/qwenpaw/clawmgt/task/*.java`: task strategy interface, registry, task service, pull service, concurrency checker.
- `src/main/java/com/qwenpaw/clawmgt/task/strategy/*.java`: chat, Skill install, Skill upgrade, Skill remove, parameter update strategies.
- `src/main/java/com/qwenpaw/clawmgt/report/*.java`: report strategy interface, registry, report service.
- `src/main/java/com/qwenpaw/clawmgt/report/strategy/SkillMetadataReportStrategy.java`: first-phase report handler.
- `src/main/java/com/qwenpaw/clawmgt/node/*.java`: channel and node services.
- `src/main/java/com/qwenpaw/clawmgt/conversation/*.java`: session, message, and chat task orchestration.
- `src/main/java/com/qwenpaw/clawmgt/api/controller/*.java`: REST controllers.
- `src/test/java/com/qwenpaw/clawmgt/**/*.java`: focused unit and slice tests.

---

### Task 1: Scaffold Spring Boot Project

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/qwenpaw/clawmgt/ClawMgtApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/test/java/com/qwenpaw/clawmgt/ClawMgtApplicationTests.java`

- [ ] **Step 1: Create Maven project file**

Create `pom.xml` with Java 21, Spring Boot 3.3.5, web, validation, data-jpa, flyway, H2, PostgreSQL runtime, Lombok optional, and test dependencies.

- [ ] **Step 2: Create application entrypoint**

```java
package com.qwenpaw.clawmgt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ClawMgtApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClawMgtApplication.class, args);
    }
}
```

- [ ] **Step 3: Create local configuration**

`application.yml` must configure H2, Flyway, JPA validation, and `clawmgt.tasks.concurrency` defaults matching the design.

- [ ] **Step 4: Write smoke test**

```java
package com.qwenpaw.clawmgt;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ClawMgtApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 5: Run smoke test**

Run: `mvn test -Dtest=ClawMgtApplicationTests`

Expected: build succeeds and the Spring context loads.

- [ ] **Step 6: Commit**

```bash
git add pom.xml src/main src/test
git commit -m "chore: scaffold Spring Boot backend"
```

---

### Task 2: Add Flyway Schema

**Files:**
- Create: `src/main/resources/db/migration/V1__init_schema.sql`
- Test: `src/test/java/com/qwenpaw/clawmgt/SchemaMigrationTests.java`

- [ ] **Step 1: Write migration test**

```java
package com.qwenpaw.clawmgt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SchemaMigrationTests {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesCoreTables() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name in " +
                        "('CHANNELS','NODES','TASKS','TASK_ITEMS','TASK_ITEM_DETAILS','TASK_EVENTS','SESSIONS','MESSAGES','NODE_REPORTS','NODE_SKILL_METADATA')",
                Integer.class);
        assertThat(count).isEqualTo(10);
    }
}
```

- [ ] **Step 2: Run migration test before schema**

Run: `mvn test -Dtest=SchemaMigrationTests`

Expected: fails because tables do not exist.

- [ ] **Step 3: Create schema**

Create tables from the approved design: `channels`, `nodes`, `tasks`, `task_items`, `task_item_details`, `task_events`, `sessions`, `messages`, `node_reports`, `node_skill_metadata`. Do not add task counter columns or task concurrency columns.

- [ ] **Step 4: Run migration test**

Run: `mvn test -Dtest=SchemaMigrationTests`

Expected: passes with 10 core tables present.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/db/migration/V1__init_schema.sql src/test/java/com/qwenpaw/clawmgt/SchemaMigrationTests.java
git commit -m "feat: add initial database schema"
```

---

### Task 3: Add Domain Enums, Entities, And Repositories

**Files:**
- Create: `src/main/java/com/qwenpaw/clawmgt/domain/enums/TaskType.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/domain/enums/TaskCategory.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/domain/enums/TaskStatus.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/domain/enums/TaskDetailStatus.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/domain/enums/ReportType.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/domain/enums/NodeStatus.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/domain/enums/MessageRole.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/domain/entity/*.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/domain/repository/*.java`
- Test: `src/test/java/com/qwenpaw/clawmgt/domain/RepositoryMappingTests.java`

- [ ] **Step 1: Write repository mapping test**

Test persisting a channel, node, parent task, task item, task item detail, session, message, node report, and Skill metadata.

- [ ] **Step 2: Run mapping test before implementation**

Run: `mvn test -Dtest=RepositoryMappingTests`

Expected: compilation fails because domain classes do not exist.

- [ ] **Step 3: Create enums**

Use lowercase persisted values through explicit `code` fields or uppercase enum names consistently. Include task types `CHAT`, `SKILL_INSTALL`, `SKILL_UPGRADE`, `SKILL_REMOVE`, `THIRD_PARTY_INSTALL`, `PARAM_UPDATE`, `CLAW_UPGRADE`, `RUNTIME_INSTALL`, `MODEL_UPDATE`, `AGENT_ROLE_UPDATE`. Include report types with `SKILL_METADATA` handled in first phase and other values reserved.

- [ ] **Step 4: Create entities**

Use JPA entities matching the schema. Keep JSON payload columns as `@Lob String`. Do not map relationships eagerly; repositories can query by IDs.

- [ ] **Step 5: Create repositories**

Repositories must include these query methods:

```java
List<NodeEntity> findByChannelIdAndStatus(Long channelId, NodeStatus status);
List<TaskItemEntity> findByTaskIdOrderByIdAsc(Long taskId);
List<TaskItemEntity> findByNodeIdAndStatusIn(Long nodeId, Collection<TaskStatus> statuses);
List<TaskItemEntity> findByChannelIdAndNodeIdAndStatus(Long channelId, Long nodeId, TaskStatus status);
Optional<NodeSkillMetadataEntity> findByNodeIdAndSkillName(Long nodeId, String skillName);
```

- [ ] **Step 6: Run mapping test**

Run: `mvn test -Dtest=RepositoryMappingTests`

Expected: passes and verifies persisted fields can be read back.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/qwenpaw/clawmgt/domain src/test/java/com/qwenpaw/clawmgt/domain
git commit -m "feat: add domain model"
```

---

### Task 4: Add Typed DTOs And Payload Binding

**Files:**
- Create: `src/main/java/com/qwenpaw/clawmgt/api/payload/task/TaskPayload.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/api/payload/task/ChatTaskPayload.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/api/payload/task/SkillInstallPayload.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/api/payload/task/SkillUpgradePayload.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/api/payload/task/SkillRemovePayload.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/api/payload/task/ParamUpdatePayload.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/api/payload/report/ReportPayload.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/api/payload/report/SkillMetadataReportPayload.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/api/dto/request/CreateTaskRequest.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/api/dto/request/ReportDataRequest.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/config/JacksonConfig.java`
- Test: `src/test/java/com/qwenpaw/clawmgt/api/PayloadBindingTests.java`

- [ ] **Step 1: Write binding tests**

Verify `type=skill_install` binds to `SkillInstallPayload`, `type=chat` binds to `ChatTaskPayload`, `type=skill_metadata` binds to `SkillMetadataReportPayload`, and malformed payloads fail Bean Validation.

- [ ] **Step 2: Run binding tests before implementation**

Run: `mvn test -Dtest=PayloadBindingTests`

Expected: compilation fails because DTOs do not exist.

- [ ] **Step 3: Create payload interfaces and concrete payloads**

Use validation annotations on all required fields. Do not accept a request body as `JSONObject`, `JsonNode`, or `Map`. Parameter objects can contain a constrained `Map<String, String>` only inside typed domain payloads.

- [ ] **Step 4: Implement Jackson polymorphic binding**

Use a custom deserializer for request envelopes if external-property binding is clearer. The final controller request type must remain `CreateTaskRequest` and `ReportDataRequest` with typed `TaskPayload` and `ReportPayload`.

- [ ] **Step 5: Run binding tests**

Run: `mvn test -Dtest=PayloadBindingTests`

Expected: all payload binding and validation assertions pass.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/qwenpaw/clawmgt/api src/main/java/com/qwenpaw/clawmgt/config src/test/java/com/qwenpaw/clawmgt/api
git commit -m "feat: add typed payload binding"
```

---

### Task 5: Add Common API Response And Error Handling

**Files:**
- Create: `src/main/java/com/qwenpaw/clawmgt/common/ApiResponse.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/common/BusinessException.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/common/GlobalExceptionHandler.java`
- Test: `src/test/java/com/qwenpaw/clawmgt/common/GlobalExceptionHandlerTests.java`

- [ ] **Step 1: Write exception handler tests**

Assert validation errors return HTTP 400 and business exceptions return their configured status.

- [ ] **Step 2: Implement common classes**

`ApiResponse<T>` should expose `success`, `code`, `message`, and `data`. `BusinessException` should carry an HTTP status and message.

- [ ] **Step 3: Run tests**

Run: `mvn test -Dtest=GlobalExceptionHandlerTests`

Expected: passes.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/qwenpaw/clawmgt/common src/test/java/com/qwenpaw/clawmgt/common
git commit -m "feat: add API error handling"
```

---

### Task 6: Implement Node, Channel, And Skill Metadata Report Services

**Files:**
- Create: `src/main/java/com/qwenpaw/clawmgt/node/ChannelService.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/node/NodeService.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/report/ReportStrategy.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/report/ReportStrategyRegistry.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/report/ReportService.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/report/strategy/SkillMetadataReportStrategy.java`
- Test: `src/test/java/com/qwenpaw/clawmgt/report/SkillMetadataReportServiceTests.java`

- [ ] **Step 1: Write report service tests**

Create a node, submit `skill_metadata` with two Skills, assert `node_reports` has one audit record and `node_skill_metadata` has two upserted rows. Submit changed version for one Skill and assert it updates.

- [ ] **Step 2: Implement channel and node services**

Channel service creates and reads channels. Node service registers nodes by channel, updates heartbeat by node ID, and verifies node-channel ownership.

- [ ] **Step 3: Implement report strategy and registry**

Only register `SkillMetadataReportStrategy` in first phase. Reserved report enum values must not have concrete strategy beans.

- [ ] **Step 4: Implement report service**

Persist a raw `node_reports` row, then delegate typed payload handling to the strategy. Reject report types without a registered strategy with a business error.

- [ ] **Step 5: Run report tests**

Run: `mvn test -Dtest=SkillMetadataReportServiceTests`

Expected: passes.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/qwenpaw/clawmgt/node src/main/java/com/qwenpaw/clawmgt/report src/test/java/com/qwenpaw/clawmgt/report
git commit -m "feat: add node reports"
```

---

### Task 7: Implement Task Strategies And Task Creation

**Files:**
- Create: `src/main/java/com/qwenpaw/clawmgt/task/TaskStrategy.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/task/TaskStrategyRegistry.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/task/TaskService.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/task/strategy/ChatTaskStrategy.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/task/strategy/SkillInstallTaskStrategy.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/task/strategy/SkillUpgradeTaskStrategy.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/task/strategy/SkillRemoveTaskStrategy.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/task/strategy/ParamUpdateTaskStrategy.java`
- Test: `src/test/java/com/qwenpaw/clawmgt/task/TaskCreationServiceTests.java`

- [ ] **Step 1: Write task creation tests**

Cover multi-node Skill install, multi-Skill detail rows, lower-version rejection, target node channel ownership, and `targetNodeIds` omitted using eligible online nodes.

- [ ] **Step 2: Implement strategy contract**

The strategy contract should return task category, normalized parent payload, node dispatch payload, task-item detail rows, and pre-dispatch validation results.

- [ ] **Step 3: Implement Skill install and upgrade strategies**

Compare requested versions against `node_skill_metadata`. Store rejected lower-version Skills as `task_item_details` with status `REJECTED` and omit them from `dispatch_payload`.

- [ ] **Step 4: Implement Skill remove and parameter update strategies**

Persist strongly typed payloads and build node dispatch payloads. Add detail rows when the payload contains multiple target units.

- [ ] **Step 5: Implement chat strategy**

Build one chat task item for the selected or resolved node under the channel.

- [ ] **Step 6: Implement task service**

Create parent `tasks`, child `task_items`, and `task_item_details` in one transaction. Compute parent status from child item statuses.

- [ ] **Step 7: Run task creation tests**

Run: `mvn test -Dtest=TaskCreationServiceTests`

Expected: passes.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/qwenpaw/clawmgt/task src/test/java/com/qwenpaw/clawmgt/task
git commit -m "feat: add task creation strategies"
```

---

### Task 8: Implement Pull, Events, Finish, Cancellation, And Concurrency

**Files:**
- Create: `src/main/java/com/qwenpaw/clawmgt/config/TaskConcurrencyProperties.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/task/TaskConcurrencyChecker.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/task/TaskPullService.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/task/TaskEventService.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/task/TaskLifecycleService.java`
- Test: `src/test/java/com/qwenpaw/clawmgt/task/TaskPullConcurrencyTests.java`
- Test: `src/test/java/com/qwenpaw/clawmgt/task/TaskLifecycleServiceTests.java`

- [ ] **Step 1: Write concurrency tests**

Assert `MUTEX_GROUP` blocks same-group lifecycle tasks, `PARALLEL` allows compatible lifecycle tasks, `EXCLUSIVE_NODE` blocks incompatible active task items, and chat tasks are parallel by default.

- [ ] **Step 2: Implement concurrency properties**

Bind `clawmgt.tasks.concurrency.types.<taskType>.group` and `.mode` from `application.yml`. Validate modes at startup.

- [ ] **Step 3: Implement pull service**

Resolve the calling node from node identity, verify it belongs to channel, query pending items by channel and node, filter through `TaskConcurrencyChecker`, and update returned items to `PULLED`.

- [ ] **Step 4: Implement events service**

Persist `task_events`. When event content represents assistant, tool, or system chat output, append a `messages` row for the session.

- [ ] **Step 5: Implement finish and cancellation**

Allow finish to `SUCCEEDED`, `PARTIAL_SUCCEEDED`, `FAILED`, or `CANCELLED`. Parent status is recalculated from task items. Cancel/delete operations reject running items.

- [ ] **Step 6: Run lifecycle tests**

Run: `mvn test -Dtest=TaskPullConcurrencyTests,TaskLifecycleServiceTests`

Expected: passes.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/qwenpaw/clawmgt/config src/main/java/com/qwenpaw/clawmgt/task src/test/java/com/qwenpaw/clawmgt/task
git commit -m "feat: add task pull lifecycle"
```

---

### Task 9: Implement Conversation Services

**Files:**
- Create: `src/main/java/com/qwenpaw/clawmgt/conversation/ConversationService.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/api/dto/request/CreateSessionRequest.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/api/dto/request/CreateMessageRequest.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/api/dto/response/SessionResponse.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/api/dto/response/MessageResponse.java`
- Test: `src/test/java/com/qwenpaw/clawmgt/conversation/ConversationServiceTests.java`

- [ ] **Step 1: Write conversation tests**

Assert creating a session under a channel works, posting a user message creates a message row and a `chat` task, and subsequent messages reuse `session.nodeId` when set.

- [ ] **Step 2: Implement conversation service**

Create sessions, list sessions, list messages, and create chat tasks through `TaskService` when a user message is posted.

- [ ] **Step 3: Run conversation tests**

Run: `mvn test -Dtest=ConversationServiceTests`

Expected: passes.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/qwenpaw/clawmgt/conversation src/main/java/com/qwenpaw/clawmgt/api/dto src/test/java/com/qwenpaw/clawmgt/conversation
git commit -m "feat: add conversation workflow"
```

---

### Task 10: Implement REST Controllers

**Files:**
- Create: `src/main/java/com/qwenpaw/clawmgt/api/controller/ChannelController.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/api/controller/TaskController.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/api/controller/ConversationController.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/api/controller/EdgeNodeController.java`
- Create: `src/main/java/com/qwenpaw/clawmgt/api/controller/EdgeTaskController.java`
- Test: `src/test/java/com/qwenpaw/clawmgt/api/ControllerIntegrationTests.java`

- [ ] **Step 1: Write controller integration tests**

Use MockMvc to cover channel-scoped node registration, node heartbeat, Skill metadata report, task creation, task list with computed counters, channel-scoped pull, event push, finish, session creation, and message creation.

- [ ] **Step 2: Implement management controllers**

Expose `POST /api/tasks`, `GET /api/tasks`, `GET /api/tasks/{taskId}`, cancel/delete endpoints, and conversation endpoints.

- [ ] **Step 3: Implement edge controllers**

Expose `POST /api/claw/channels/{channelId}/nodes/register`, `POST /api/claw/nodes/{nodeId}/heartbeat`, `POST /api/claw/nodes/{nodeId}/reports`, `GET /api/claw/channels/{channelId}/tasks/pull`, `POST /api/claw/task-items/{taskItemId}/events`, and `POST /api/claw/task-items/{taskItemId}/finish`.

- [ ] **Step 4: Run controller tests**

Run: `mvn test -Dtest=ControllerIntegrationTests`

Expected: passes.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/qwenpaw/clawmgt/api/controller src/test/java/com/qwenpaw/clawmgt/api
git commit -m "feat: add REST APIs"
```

---

### Task 11: Final Verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Add README**

Document how to run tests, run the app, access H2 console, and call the main management and edge APIs.

- [ ] **Step 2: Run full test suite**

Run: `mvn test`

Expected: all tests pass.

- [ ] **Step 3: Run package**

Run: `mvn package -DskipTests`

Expected: build succeeds and creates a jar under `target/`.

- [ ] **Step 4: Check Git status**

Run: `git status --short`

Expected: only intentionally ignored or user-owned files remain untracked. Do not add `.idea/` unless the user explicitly asks.

- [ ] **Step 5: Commit**

```bash
git add README.md
git commit -m "docs: add backend runbook"
```

---

## Self-Review Checklist

- Spec coverage: tasks, task items, details, events, sessions, messages, node reports, Skill metadata, typed payloads, channel-scoped pull, node-scoped heartbeat/reporting, and runtime concurrency are each covered by a task above.
- Deferred-work scan: plan contains no deferred implementation markers.
- Type consistency: task type names, report type names, table names, and endpoint paths match the accepted design document.
- Scope check: frontend work is excluded.
