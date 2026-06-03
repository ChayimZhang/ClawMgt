# ClawMgt Backend Design

Date: 2026-06-03

## Goal

Build a new backend-only ClawMgt project from scratch. Do not use code from `ClawMgt.rar`.

The backend manages cloud-to-edge Claw communication. It must support existing chat-style tasks and new lifecycle tasks such as Skill install, Skill upgrade, parameter update, runtime install, model update, Claw upgrade, and agent role update. The system should allow future task types with limited code changes.

## Recommended Architecture

Use a unified task model with type-specific strategies.

The cloud service stores one parent task for each user-created operation and one child task item for each target Claw node. A task strategy validates the request, normalizes payload data, decides routing and concurrency rules, and builds the payload returned to edge Claw nodes.

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

Represents a parent operation created from the management side.

Important fields:

- `id`
- `type`
- `category`
- `status`
- `title`
- `payload`
- `totalItems`
- `pendingItems`
- `runningItems`
- `succeededItems`
- `failedItems`
- `cancelledItems`
- `createdAt`
- `updatedAt`
- `completedAt`

`category` controls concurrency. Initial categories:

- `CHAT`: normal dialogue tasks
- `LIFECYCLE`: Skill install, Skill upgrade, parameter updates, runtime install, and similar management operations

### TaskItem

Represents one concrete task assigned to one node.

Important fields:

- `id`
- `taskId`
- `nodeId`
- `type`
- `category`
- `status`
- `payload`
- `result`
- `errorMessage`
- `pulledAt`
- `startedAt`
- `completedAt`
- `createdAt`
- `updatedAt`

Parent tasks aggregate their status from child task items.

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

Initial report types:

- `skill_metadata`
- `runtime_metadata`
- `model_metadata`
- `claw_metadata`
- `agent_role_metadata`

Each report type maps to a `ReportStrategy`.

The first implementation must fully handle `skill_metadata`. Other metadata report types can be wired through a generic typed metadata strategy when their payload schema is simple and explicit.

## Strategy Contract

Each strategy should answer:

- What task type does this strategy handle?
- Which category does it use?
- How should the create request payload be validated?
- How should the parent payload be normalized?
- How should each node-specific child payload be built?
- Can the task be cancelled?
- Can the task item be deleted?
- Does the task need version checks before dispatch?

The service layer should resolve strategies from a registry keyed by task type. Controllers should not contain task-type branches.

## Request Binding and Validation

Task creation uses a typed request envelope:

```java
public class CreateTaskRequest {
    @NotNull
    private TaskType type;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotEmpty
    @Size(max = 1000)
    private List<@NotNull @Positive Long> targetNodeIds;

    @Valid
    @NotNull
    private TaskPayload payload;
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

## API Design

### Management APIs

`POST /api/tasks`

Creates a task. Request shape:

```json
{
  "type": "skill_install",
  "title": "Install data-export skill",
  "targetNodeIds": [1, 2, 3],
  "payload": {
    "skillName": "data-export",
    "version": "1.2.0",
    "downloadUrl": "https://example.com/skills/data-export.zip",
    "parameters": {},
    "force": false
  }
}
```

For multiple target nodes, create one parent task and one child task item per node.

`GET /api/tasks`

Lists parent tasks with summary counters.

`GET /api/tasks/{taskId}`

Returns parent task detail with child task items.

`POST /api/tasks/{taskId}/cancel`

Cancels a parent task only when no child item is running. Pending child items become `CANCELLED`.

`POST /api/task-items/{taskItemId}/cancel`

Cancels a child task only when it is still `PENDING`.

`DELETE /api/tasks/{taskId}`

Deletes a parent task only when none of its child items are running.

`DELETE /api/task-items/{taskItemId}`

Deletes a child task only when it is not running.

### Edge Claw APIs

`POST /api/claw/nodes/register`

Registers a node and returns its assigned ID or token data.

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

`GET /api/claw/nodes/{nodeId}/tasks/pull?timeout=30&limit=10`

Long-polls pending task items for this node.

Pull behavior:

- Chat tasks can be pulled independently.
- Lifecycle tasks are returned only if the node has no lifecycle task item currently `PULLED` or `RUNNING`.
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
- `FAILED`
- `CANCELLED`
- `DELETED`

Parent task aggregation:

- All child items `SUCCEEDED`: parent `SUCCEEDED`
- Any child item `FAILED` and all child items terminal: parent `FAILED`
- All child items `CANCELLED`: parent `CANCELLED`
- Any child item `RUNNING` or `PULLED`: parent `RUNNING`
- Otherwise parent `PENDING`

Terminal statuses:

- `SUCCEEDED`
- `FAILED`
- `CANCELLED`
- `DELETED`

## Version Rules

Skill install and upgrade strategies should compare requested Skill version against node-reported metadata when available.

Default rule:

- Higher requested version: allow
- Same requested version: allow only when `force=true`
- Lower requested version: reject child task creation or mark child task failed before dispatch
- Missing node metadata: allow dispatch because the edge node can perform a final local check

The edge Claw must still enforce version safety locally because node metadata can be stale.

## Concurrency Rules

Each Claw node may run only one lifecycle task at a time.

Lifecycle tasks and chat tasks do not block each other.

The cloud service enforces this during pull by checking whether the node already has a non-terminal lifecycle task item in `PULLED` or `RUNNING`.

## Error Handling

Use validation errors for malformed requests.

Use business errors for:

- Unknown task type
- Unknown report type
- Missing target nodes
- Nonexistent node
- Invalid typed payload
- Unsupported state transition
- Cancel/delete attempts on running task items
- Version downgrade rejection

Store edge execution failures in `TaskItem.result` and `TaskItem.errorMessage`.

## Testing Plan

Backend tests should cover:

- Creating a multi-node parent task and child task items
- Strategy resolution by task type
- Skill payload validation
- Version comparison rules
- Pulling lifecycle tasks with per-node concurrency
- Chat task pull not blocked by lifecycle task
- Task item finish updates parent aggregation
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
- Task strategy interface and registry
- Report strategy interface and registry
- Strategies for `chat`, `skill_install`, `skill_upgrade`, `skill_remove`, and `param_update`
- Generic JSON lifecycle strategy support for future lifecycle task types when payload is accepted as structured JSON
- Management task APIs
- Edge Claw register, heartbeat, typed report, pull, event, and finish APIs
- Focused service tests

The frontend is explicitly out of scope for this phase.

## Repository Constraint

The directory is a Git repository. Design and implementation changes should be tracked in Git.
