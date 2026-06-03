# ClawMgt Backend Design

Date: 2026-06-03
Last updated: 2026-06-04

## Goal

Build a new backend-only ClawMgt project from scratch. Do not use code from `ClawMgt.rar`.

The backend manages cloud-to-edge Claw communication. It must support existing chat-style tasks and new lifecycle tasks such as Skill install, Skill upgrade, parameter update, runtime install, model update, Claw upgrade, and agent role update. The system should allow future task types with limited code changes.

## Recommended Architecture

Use a unified task model with type-specific strategies.

Tasks are created for a channel. A channel owns registered edge Claw nodes, and edge Claw nodes register and pull tasks by `channelId`. Heartbeat and data report APIs use `nodeId` because those calls describe the state of one concrete node.

The cloud service stores one parent task for each channel-level operation and one child task item for each target Claw node. A task strategy validates the request, normalizes payload data, decides routing and concurrency rules, builds node-specific payloads, and writes task-item details that can be expanded in the management UI.

This keeps the common lifecycle in one place while allowing each task type to own its own business rules.

Requests must use typed DTOs. Controllers must not accept one catch-all `JSONObject`, `JsonNode`, or `Map` as the request body. Dynamic task and report payloads should use Jackson polymorphic binding so the incoming `type` selects a concrete payload class with normal Bean Validation.

## Backend Stack

- Java 21
- Spring Boot 3
- Spring Web
- Spring Validation
- Spring Data JPA
- Flyway
- H2 for local development
- PostgreSQL-ready schema
- Lombok optional, but avoid relying on generated behavior where clarity matters
- Maven project layout

## Domain Model

### Channel

Represents a cloud-to-edge communication channel.

Important fields:

- `id`
- `name`
- `description`
- `status`
- `createdAt`
- `updatedAt`

All task creation and edge task pulling are scoped by channel.

### Node

Represents a registered edge Claw node.

Important fields:

- `id`
- `channelId`
- `nodeKey`
- `hostname`
- `ipAddress`
- `clawVersion`
- `status`
- `metadata`
- `lastHeartbeatAt`
- `createdAt`
- `updatedAt`

### Task

Represents a parent channel-level operation created from the management side.

Important fields:

- `id`
- `channelId`
- `type`
- `category`
- `status`
- `title`
- `payloadType`
- `payloadSchemaVersion`
- `requestPayload`
- `createdAt`
- `updatedAt`
- `completedAt`

Task item counters are not stored on `tasks`. List and detail APIs should compute totals and status counts from `task_items` with grouped queries.

`category` is a high-level classification. Initial categories:

- `CHAT`: normal dialogue tasks
- `LIFECYCLE`: Skill install, Skill upgrade, parameter updates, runtime install, and similar management operations

Concrete concurrency is controlled by task type configuration, not by `category` alone.

### TaskItem

Represents one concrete task assigned to one node.

Important fields:

- `id`
- `taskId`
- `channelId`
- `nodeId`
- `type`
- `category`
- `status`
- `payloadType`
- `payloadSchemaVersion`
- `dispatchPayload`
- `detailPayload`
- `result`
- `errorMessage`
- `pulledAt`
- `startedAt`
- `completedAt`
- `createdAt`
- `updatedAt`

Parent tasks aggregate their status from child task items.

### TaskItemDetail

Represents expandable, task-type-specific detail under a child task item.

Important fields:

- `id`
- `taskItemId`
- `detailType`
- `detailKey`
- `status`
- `payload`
- `result`
- `errorMessage`
- `createdAt`
- `updatedAt`

For Skill dispatch, one task item can have multiple detail rows, one per Skill. If a Skill is rejected because the requested version is lower than the node-reported version, the detail row is stored with status `REJECTED` and an explanatory `errorMessage`, while other Skill detail rows can still proceed.

For future task types, strategies decide whether detail rows represent packages, runtime components, model files, role definition changes, or another domain-specific unit.

### TaskEvent

Stores events pushed by edge Claw while a task item is executing.

Important fields:

- `id`
- `taskItemId`
- `eventId`
- `eventType`
- `status`
- `role`
- `content`
- `rawEvent`
- `createdAt`

### Session

Represents a chat session under one channel.

Important fields:

- `id`
- `channelId`
- `nodeId`
- `title`
- `status`
- `createdAt`
- `updatedAt`
- `completedAt`

`nodeId` can be filled when the chat task is assigned to or pulled by a concrete node. This keeps the session channel-scoped while still allowing node-specific execution tracking.

### Message

Represents a chat message under a session.

Important fields:

- `id`
- `sessionId`
- `taskId`
- `taskItemId`
- `source`
- `role`
- `content`
- `rawPayload`
- `createdAt`

User messages are stored when a chat task is created. Assistant, tool, and system messages can be stored from task events or finish callbacks.

### NodeSkillMetadata

Stores Skill metadata reported from edge Claw nodes through the generic report API.

Important fields:

- `id`
- `nodeId`
- `skillName`
- `version`
- `parameters`
- `reportedAt`

Use an upsert by `(nodeId, skillName)`.

### NodeReport

Stores raw report envelopes for auditable data reported by edge Claw nodes.

Important fields:

- `id`
- `nodeId`
- `reportType`
- `payload`
- `status`
- `errorMessage`
- `reportedAt`
- `createdAt`

Report-specific tables, such as `node_skill_metadata`, are updated by report strategies.

## Payload Storage

Payload data is stored deliberately at multiple levels:

- `tasks.request_payload`: the original typed request payload serialized as JSON. This preserves the user-created intent for audit, retry, and display.
- `task_items.dispatch_payload`: the node-specific executable payload returned by the pull API. Strategies can remove irrelevant data or add node-specific fields.
- `task_items.detail_payload`: an optional compact JSON summary for child-row display.
- `task_item_details.payload`: repeated detail units for expandable views, such as each Skill in a multi-Skill dispatch.
- `task_events.raw_event`: raw event data pushed from edge Claw.
- `messages.raw_payload`: raw chat message data.

The API layer still uses strongly typed DTOs. JSON persistence is used for durable, versioned domain payloads after validation and strategy normalization, not as a replacement for typed request objects.

Payload records should include a `payload_type` and `payload_schema_version` on `tasks` and `task_items`. This gives future migrations a clear hook when payload classes change.

## Physical Table Design

Use Flyway to create these initial tables.

### `channels`

- `id BIGINT PRIMARY KEY`
- `name VARCHAR(100) NOT NULL`
- `description VARCHAR(500)`
- `status VARCHAR(30) NOT NULL`
- `created_at TIMESTAMP NOT NULL`
- `updated_at TIMESTAMP NOT NULL`

### `nodes`

- `id BIGINT PRIMARY KEY`
- `channel_id BIGINT NOT NULL`
- `node_key VARCHAR(100) NOT NULL`
- `hostname VARCHAR(255)`
- `ip_address VARCHAR(64)`
- `claw_version VARCHAR(50)`
- `status VARCHAR(30) NOT NULL`
- `metadata TEXT`
- `last_heartbeat_at TIMESTAMP`
- `created_at TIMESTAMP NOT NULL`
- `updated_at TIMESTAMP NOT NULL`

Indexes:

- `(channel_id, node_key)` unique
- `(channel_id, status)`

### `tasks`

- `id BIGINT PRIMARY KEY`
- `channel_id BIGINT NOT NULL`
- `type VARCHAR(50) NOT NULL`
- `category VARCHAR(30) NOT NULL`
- `status VARCHAR(30) NOT NULL`
- `title VARCHAR(200) NOT NULL`
- `payload_type VARCHAR(100) NOT NULL`
- `payload_schema_version INT NOT NULL`
- `request_payload TEXT NOT NULL`
- `created_at TIMESTAMP NOT NULL`
- `updated_at TIMESTAMP NOT NULL`
- `completed_at TIMESTAMP`

Indexes:

- `(channel_id, status)`
- `(channel_id, type, created_at)`

`request_payload` stores the validated and normalized parent payload. For a Skill install task, it stores the full typed list of Skills selected by the user.

Task summary counts are derived from `task_items` at query time, for example `count(*) grouped by status where task_id in (...)`. Do not duplicate these counts in the `tasks` table.

### `task_items`

- `id BIGINT PRIMARY KEY`
- `task_id BIGINT NOT NULL`
- `channel_id BIGINT NOT NULL`
- `node_id BIGINT NOT NULL`
- `type VARCHAR(50) NOT NULL`
- `category VARCHAR(30) NOT NULL`
- `status VARCHAR(30) NOT NULL`
- `payload_type VARCHAR(100) NOT NULL`
- `payload_schema_version INT NOT NULL`
- `dispatch_payload TEXT NOT NULL`
- `detail_payload TEXT`
- `result TEXT`
- `error_message TEXT`
- `pulled_at TIMESTAMP`
- `started_at TIMESTAMP`
- `completed_at TIMESTAMP`
- `created_at TIMESTAMP NOT NULL`
- `updated_at TIMESTAMP NOT NULL`

Indexes:

- `(channel_id, node_id, status)`
- `(node_id, type, status)`
- `(node_id, category, status)`
- `(task_id, status)`

`dispatch_payload` stores the node-specific payload returned to edge Claw by the pull API. For Skill install, rejected lower-version Skills should already be removed from this payload.

`detail_payload` stores a compact typed summary for child-row display.

### `task_item_details`

- `id BIGINT PRIMARY KEY`
- `task_item_id BIGINT NOT NULL`
- `detail_type VARCHAR(50) NOT NULL`
- `detail_key VARCHAR(200) NOT NULL`
- `status VARCHAR(30) NOT NULL`
- `payload TEXT NOT NULL`
- `result TEXT`
- `error_message TEXT`
- `created_at TIMESTAMP NOT NULL`
- `updated_at TIMESTAMP NOT NULL`

Indexes:

- `(task_item_id, status)`
- `(detail_type, detail_key)`

For Skill install, `detail_type=skill`, `detail_key=skillName`, and `payload` stores the typed Skill package data for that Skill on that node. Version rejection and execution results are stored per detail row.

### `task_events`

- `id BIGINT PRIMARY KEY`
- `task_id BIGINT NOT NULL`
- `task_item_id BIGINT NOT NULL`
- `event_id VARCHAR(100) NOT NULL`
- `event_type VARCHAR(100) NOT NULL`
- `status VARCHAR(30)`
- `role VARCHAR(50)`
- `content TEXT`
- `raw_event TEXT`
- `created_at TIMESTAMP NOT NULL`

Indexes:

- `(task_item_id, id)`
- `(task_id, id)`
- `(event_id)` unique when edge Claw event IDs are globally unique

### `sessions`

- `id BIGINT PRIMARY KEY`
- `channel_id BIGINT NOT NULL`
- `node_id BIGINT`
- `title VARCHAR(200)`
- `status VARCHAR(30) NOT NULL`
- `created_at TIMESTAMP NOT NULL`
- `updated_at TIMESTAMP NOT NULL`
- `completed_at TIMESTAMP`

Indexes:

- `(channel_id, status, updated_at)`
- `(node_id, updated_at)`

### `messages`

- `id BIGINT PRIMARY KEY`
- `session_id BIGINT NOT NULL`
- `task_id BIGINT`
- `task_item_id BIGINT`
- `source VARCHAR(50) NOT NULL`
- `role VARCHAR(50) NOT NULL`
- `content TEXT`
- `raw_payload TEXT`
- `created_at TIMESTAMP NOT NULL`

Indexes:

- `(session_id, id)`
- `(task_id)`
- `(task_item_id)`

### `node_reports`

- `id BIGINT PRIMARY KEY`
- `node_id BIGINT NOT NULL`
- `report_type VARCHAR(50) NOT NULL`
- `payload_type VARCHAR(100) NOT NULL`
- `payload_schema_version INT NOT NULL`
- `payload TEXT NOT NULL`
- `status VARCHAR(30) NOT NULL`
- `error_message TEXT`
- `reported_at TIMESTAMP NOT NULL`
- `created_at TIMESTAMP NOT NULL`

Indexes:

- `(node_id, report_type, reported_at)`

### `node_skill_metadata`

- `id BIGINT PRIMARY KEY`
- `node_id BIGINT NOT NULL`
- `skill_name VARCHAR(100) NOT NULL`
- `version VARCHAR(50) NOT NULL`
- `parameters TEXT`
- `reported_at TIMESTAMP NOT NULL`
- `created_at TIMESTAMP NOT NULL`
- `updated_at TIMESTAMP NOT NULL`

Indexes:

- `(node_id, skill_name)` unique
- `(skill_name, version)`

## Task Types

Initial task types:

- `chat`
- `skill_install`
- `skill_upgrade`
- `skill_remove`
- `third_party_install`
- `param_update`
- `claw_upgrade`
- `runtime_install`
- `model_update`
- `agent_role_update`

Each task type maps to a `TaskStrategy`.

## Report Types

Initial report type implementation:

- `skill_metadata`

Reserved report type enum values for future extension:

- `runtime_metadata`
- `model_metadata`
- `claw_metadata`
- `agent_role_metadata`

Each report type maps to a `ReportStrategy`.

The first implementation only registers and handles `skill_metadata`. Other report type enum values are reserved extension points; do not build their concrete payload classes, strategies, or business handling until a real reporting requirement appears.

## Strategy Contract

Each strategy should answer:

- What task type does this strategy handle?
- Which category does it use?
- How should the create request payload be validated?
- How should the parent payload be normalized?
- How should each node-specific child payload be built?
- How should task-item detail rows be built?
- Can the task be cancelled?
- Can the task item be deleted?
- Does the task need version checks before dispatch?

The service layer should resolve strategies from a registry keyed by task type. Controllers should not contain task-type branches.

## Request Binding and Validation

Task creation uses a typed request envelope:

```java
public class CreateTaskRequest {
    @NotNull
    @Positive
    private Long channelId;

    @NotNull
    private TaskType type;

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 1000)
    private List<@NotNull @Positive Long> targetNodeIds;

    @Valid
    @NotNull
    private TaskPayload payload;
}
```

`targetNodeIds` is optional for task types that support channel-wide dispatch. When omitted, the strategy resolves all eligible nodes under the channel. When present, every node must belong to `channelId`.

Chat task creation uses the same envelope with a chat payload:

```java
public class ChatTaskPayload implements TaskPayload {
    @Positive
    private Long sessionId;

    @NotBlank
    @Size(max = 4000)
    private String content;

    @Size(max = 200)
    private String title;
}
```

Skill dispatch supports multiple Skills in one parent task:

```java
public class SkillInstallPayload implements TaskPayload {
    @NotEmpty
    @Size(max = 1000)
    private List<@Valid SkillPackagePayload> skills;

    private boolean force;
}

public class SkillPackagePayload {
    @NotBlank
    @Size(max = 100)
    private String skillName;

    @NotBlank
    @Size(max = 50)
    private String version;

    @NotNull
    private URI downloadUrl;

    @Valid
    private SkillParametersPayload parameters;
}
```

`TaskPayload` is an interface. Jackson selects the concrete payload from the sibling `type` field, for example `SkillInstallPayload`, `SkillRemovePayload`, or `ParamUpdatePayload`. The implementation can use `@JsonTypeInfo(include = As.EXTERNAL_PROPERTY, property = "type")`, a custom deserializer, or an equivalent Jackson module, but the controller-facing DTO remains strongly typed.

Data reporting uses the same shape:

```java
public class ReportDataRequest {
    @NotNull
    private ReportType type;

    @Valid
    @NotNull
    private ReportPayload payload;
}
```

`ReportPayload` is also polymorphic. For example, `skill_metadata` binds to `SkillMetadataReportPayload`, which contains a validated list of Skill metadata items.

Basic validation rules:

- Required IDs must be positive.
- Names and type strings must have sane maximum lengths.
- Versions must be nonblank for versioned artifacts.
- URLs must be valid absolute URLs when download is required.
- Lists must have a bounded size.
- Free-form parameter keys should have bounded length and a safe character pattern.
- Payload objects should reject missing required fields before reaching strategy logic.

Open-ended business fields are allowed only inside a typed payload where they are part of the domain, such as Skill parameters. They should not replace the typed payload object itself.

## Chat Task Flow

Chat is a first-class task type, not a separate one-off implementation.

Flow:

1. The management side creates or reuses a `session` under a `channel`.
2. The user message is stored in `messages`.
3. The chat service creates a parent `task` with `type=chat`, `category=CHAT`, `channel_id=session.channel_id`, and a typed `ChatTaskPayload`.
4. The chat strategy creates one `task_item`. If a concrete node is already chosen, the item is assigned to that node. Otherwise, the strategy can assign an eligible node under the channel according to the routing rule chosen for the first implementation.
5. Edge Claw pulls the task by channel, executes the chat through its CloudChannel or equivalent local channel, and pushes stream events into `task_events`.
6. The backend stores assistant/tool/system content from events into `messages` when appropriate.
7. On finish, the task item and parent task are completed, and the session's `updated_at` and status are refreshed.

Initial routing rule:

- If `targetNodeIds` contains exactly one node, use that node.
- If `session.nodeId` is already set, continue routing the session to that node.
- Otherwise choose the first eligible online node under the channel, set `session.nodeId`, and create the child task item for that node.

This keeps multi-turn chat stable while still keeping task pulling scoped by channel.

## API Design

### Management APIs

`POST /api/tasks`

Creates a task. Request shape:

```json
{
  "channelId": 10,
  "type": "skill_install",
  "title": "Install Skills",
  "targetNodeIds": [1, 2, 3],
  "payload": {
    "skills": [
      {
        "skillName": "data-export",
        "version": "1.2.0",
        "downloadUrl": "https://example.com/skills/data-export.zip",
        "parameters": {}
      },
      {
        "skillName": "report-writer",
        "version": "2.0.0",
        "downloadUrl": "https://example.com/skills/report-writer.zip",
        "parameters": {}
      }
    ],
    "force": false
  }
}
```

For multiple target nodes, create one channel-scoped parent task and one child task item per node. For multi-unit payloads such as multi-Skill dispatch, create task-item detail rows under each child task item.

If `targetNodeIds` is omitted, the strategy can create child task items for all eligible online nodes under `channelId`.

`GET /api/tasks`

Lists parent tasks with summary counters computed from associated task items.

Supports filtering by `channelId`, `type`, and `status`.

`GET /api/tasks/{taskId}`

Returns parent task detail with child task items and expandable task-item details.

`POST /api/tasks/{taskId}/cancel`

Cancels a parent task only when no child item is running. Pending child items become `CANCELLED`.

`POST /api/task-items/{taskItemId}/cancel`

Cancels a child task only when it is still `PENDING`.

`DELETE /api/tasks/{taskId}`

Deletes a parent task only when none of its child items are running.

`DELETE /api/task-items/{taskItemId}`

Deletes a child task only when it is not running.

### Conversation APIs

`POST /api/channels/{channelId}/sessions`

Creates a chat session under the channel.

`GET /api/channels/{channelId}/sessions`

Lists chat sessions under the channel.

`GET /api/sessions/{sessionId}/messages`

Lists stored messages for a session.

`POST /api/sessions/{sessionId}/messages`

Creates a user message and a `chat` task under the session's channel. The chat task follows the same task lifecycle as other tasks and writes task events and assistant messages as edge Claw reports progress.

### Edge Claw APIs

`POST /api/claw/channels/{channelId}/nodes/register`

Registers a node under the channel and returns its assigned node ID or token data.

`POST /api/claw/nodes/{nodeId}/heartbeat`

Updates node heartbeat only. It must not accept Skill metadata or other reported data.

`POST /api/claw/nodes/{nodeId}/reports`

Accepts typed data reports from edge Claw nodes. Request shape:

```json
{
  "type": "skill_metadata",
  "payload": {
    "skills": [
      {
        "skillName": "data-export",
        "version": "1.2.0",
        "parameters": {}
      }
    ]
  }
}
```

The report service resolves a `ReportStrategy` by report type. For `skill_metadata`, it upserts `node_skill_metadata`.

`GET /api/claw/channels/{channelId}/tasks/pull?timeout=30&limit=10`

Long-polls pending task items for the calling node under the channel.

The pull API is channel-scoped. The service must still resolve the concrete node from node authentication, such as a node token issued at registration time or an explicit node identity header. The node identity is needed to enforce per-node concurrency and return child task items assigned to that node, but `nodeId` is not part of the pull path.

Pull behavior:

- The node must belong to the requested channel.
- Chat tasks can be pulled independently.
- Lifecycle tasks are returned only when the candidate task type's resolved concurrency configuration allows it to run with the node's current `PULLED` or `RUNNING` task items.
- Pulled task items move from `PENDING` to `PULLED`.

`POST /api/claw/task-items/{taskItemId}/events`

Stores execution events from edge Claw.

`POST /api/claw/task-items/{taskItemId}/finish`

Finishes a task item with `SUCCEEDED`, `FAILED`, or `CANCELLED`. The service recalculates parent task status after every child update.

## Status Model

Task and task item statuses:

- `PENDING`
- `PULLED`
- `RUNNING`
- `SUCCEEDED`
- `PARTIAL_SUCCEEDED`
- `FAILED`
- `CANCELLED`
- `DELETED`

Task item detail statuses:

- `PENDING`
- `RUNNING`
- `SUCCEEDED`
- `REJECTED`
- `FAILED`
- `CANCELLED`

Parent task aggregation:

- All child items `SUCCEEDED`: parent `SUCCEEDED`
- Any child item `PARTIAL_SUCCEEDED` and all child items terminal: parent `PARTIAL_SUCCEEDED`
- Any child item `FAILED` and all child items terminal: parent `FAILED`
- All child items `CANCELLED`: parent `CANCELLED`
- Any child item `RUNNING` or `PULLED`: parent `RUNNING`
- Otherwise parent `PENDING`

Terminal statuses:

- `SUCCEEDED`
- `PARTIAL_SUCCEEDED`
- `FAILED`
- `CANCELLED`
- `DELETED`

## Version Rules

Skill install and upgrade strategies should compare each requested Skill version against node-reported metadata when available.

Default rule:

- Higher requested version: allow
- Same requested version: allow only when `force=true`
- Lower requested version: create a task item detail row with status `REJECTED`, store the reason, and omit that Skill from the node's executable dispatch payload
- Missing node metadata: allow dispatch because the edge node can perform a final local check

If all Skill details for a task item are rejected before dispatch, mark the task item `FAILED` with a clear `errorMessage`. If some Skill details are rejected and some execute successfully, mark the task item `PARTIAL_SUCCEEDED`.

The edge Claw must still enforce version safety locally because node metadata can be stale.

## Concurrency Rules

Lifecycle tasks are not globally serialized by default. Some lifecycle tasks can run concurrently, and some must run one by one. The backend must make this controllable in code and configurable in `application.yaml`.

Concurrency is a runtime rule, not persisted task data. The database stores only task facts such as type, category, node, status, and timestamps. During pull, the service resolves the candidate task type against the current code and `application.yaml` configuration, so changing concurrency behavior does not require data migration or rewriting existing task rows.

Each task type resolves at runtime to:

- `concurrencyGroup`: a named group such as `chat`, `skill-management`, `runtime-management`, or `node-exclusive`.
- `concurrencyMode`: how this task interacts with other in-flight task items on the same node.

Initial modes:

- `PARALLEL`: does not block or get blocked by other task items.
- `MUTEX_GROUP`: only one non-terminal task item in the same `concurrencyGroup` can be `PULLED` or `RUNNING` on the same node.
- `EXCLUSIVE_NODE`: no other non-terminal task item, except `PARALLEL` chat tasks when explicitly allowed, can be `PULLED` or `RUNNING` on the same node.

Default examples:

- `chat`: `group=chat`, `mode=PARALLEL`
- `skill_install`: `group=skill-management`, `mode=MUTEX_GROUP`
- `skill_upgrade`: `group=skill-management`, `mode=MUTEX_GROUP`
- `skill_remove`: `group=skill-management`, `mode=MUTEX_GROUP`
- `param_update`: `group=param-management`, `mode=PARALLEL`
- `runtime_install`: `group=runtime-management`, `mode=MUTEX_GROUP`
- `model_update`: `group=model-management`, `mode=MUTEX_GROUP`
- `claw_upgrade`: `group=node-exclusive`, `mode=EXCLUSIVE_NODE`

The service enforces this during pull. Before moving a task item to `PULLED`, it checks active task items on the same node, resolves concurrency rules for the candidate and active task types from the current configuration, and returns only task items that can run under that configuration.

`application.yaml` should support overrides:

```yaml
clawmgt:
  tasks:
    concurrency:
      defaults:
        lifecycle-mode: MUTEX_GROUP
      types:
        chat:
          group: chat
          mode: PARALLEL
        skill_install:
          group: skill-management
          mode: MUTEX_GROUP
        skill_upgrade:
          group: skill-management
          mode: MUTEX_GROUP
        param_update:
          group: param-management
          mode: PARALLEL
        claw_upgrade:
          group: node-exclusive
          mode: EXCLUSIVE_NODE
```

Configuration should be validated at startup. Unknown task types should fail fast. Missing lifecycle task configuration should fall back to the strategy default, and truly unknown future lifecycle task types should use a conservative `MUTEX_GROUP` default unless explicitly configured.

Chat tasks and lifecycle tasks do not block each other by default because `chat` uses `PARALLEL`. If a future operation must block chat, configure it as `EXCLUSIVE_NODE` and disallow parallel chat for that operation in the concurrency checker.

## Error Handling

Use validation errors for malformed requests.

Use business errors for:

- Unknown task type
- Unknown report type
- Missing target nodes
- Nonexistent node
- Node does not belong to the task channel
- Invalid typed payload
- Unsupported state transition
- Cancel/delete attempts on running task items
- Version downgrade rejection

Store edge execution failures in `TaskItem.result` and `TaskItem.errorMessage`.

## Testing Plan

Backend tests should cover:

- Creating a channel-scoped multi-node parent task and child task items
- Rejecting target nodes that do not belong to the task channel
- Strategy resolution by task type
- Skill payload validation
- Skill dispatch with multiple Skills creates task-item detail rows
- Lower Skill versions are stored as rejected detail rows and omitted from dispatch payload
- Version comparison rules
- Pulling lifecycle tasks with application-configured per-node concurrency
- `MUTEX_GROUP` blocks only same-group lifecycle tasks
- `PARALLEL` lifecycle tasks can run with other compatible lifecycle tasks
- `EXCLUSIVE_NODE` blocks incompatible task items on the same node
- Startup validation for task concurrency configuration
- Pulling tasks by channel while resolving the concrete node identity
- Chat task pull not blocked by lifecycle task
- Chat session/message creation and chat task creation
- Chat task events can append assistant messages
- Task item finish updates parent aggregation
- Partial detail success updates task item and parent status to `PARTIAL_SUCCEEDED`
- Cancel/delete state restrictions
- Heartbeat updates only heartbeat fields
- Skill metadata report upserts node Skill metadata
- Task request payload binds to concrete typed payload classes
- Report request payload binds to concrete typed payload classes
- Bean Validation rejects missing fields, invalid URLs, empty node lists, and invalid report payloads

## Implementation Scope

Initial implementation should include:

- Maven Spring Boot project scaffold
- Entities, repositories, enums, DTOs
- Flyway migration
- Channel, node, task, task item, task item detail, task event, session, message, report, and Skill metadata tables
- Task strategy interface and registry
- Report strategy interface and registry
- Task concurrency configuration properties and checker
- Strategies for `chat`, `skill_install`, `skill_upgrade`, `skill_remove`, and `param_update`
- Management task APIs
- Conversation APIs
- Edge Claw channel-scoped register and pull APIs
- Edge Claw node-scoped heartbeat, typed report, event, and finish APIs
- Focused service tests

The frontend is explicitly out of scope for this phase.

## Repository Constraint

The directory is a Git repository. Design and implementation changes should be tracked in Git.
