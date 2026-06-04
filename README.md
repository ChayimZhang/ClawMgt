# ClawMgt

ClawMgt is a Spring Boot backend for managing channel-scoped Claw nodes, typed tasks, node reports, and conversation sessions.

## Run

Requirements:

- JDK 21
- Maven 3.8+

Commands:

```powershell
mvn test
mvn spring-boot:run
```

The default profile uses an in-memory H2 database and Flyway migrations. Use `application-local.yml` as the starting point for a local PostgreSQL profile.

## Implemented Scope

- Channel-scoped node registration and task pulling.
- Node heartbeat endpoint that only updates heartbeat/status.
- Separate node report endpoint with typed `skill_metadata` payload handling.
- Typed task creation with Jackson polymorphic payload binding.
- First-phase task strategies for chat, skill install, skill upgrade, skill remove, and param update.
- Task item details for type-specific subtask results, such as per-skill installation detail.
- Conversation sessions/messages backed by chat tasks.
- Config-driven lifecycle task concurrency policy under `clawmgt.tasks.concurrency`.

Reserved task/report enums exist for later expansion, but first-phase report processing only implements `skill_metadata`.

## API Overview

Management APIs:

- `POST /api/channels`
- `POST /api/tasks`
- `GET /api/tasks?channelId={channelId}`
- `GET /api/tasks/{taskId}`
- `GET /api/tasks/items/{taskItemId}`
- `POST /api/tasks/{taskId}/cancel`
- `DELETE /api/tasks/{taskId}`
- `POST /api/tasks/items/{taskItemId}/cancel`
- `DELETE /api/tasks/items/{taskItemId}`
- `POST /api/channels/{channelId}/sessions`
- `GET /api/channels/{channelId}/sessions`
- `POST /api/sessions/{sessionId}/messages`
- `GET /api/sessions/{sessionId}/messages`

Claw node APIs:

- `POST /api/claw/channels/{channelId}/nodes/register`
- `POST /api/claw/nodes/{nodeId}/heartbeat`
- `POST /api/claw/nodes/{nodeId}/reports`
- `GET /api/claw/channels/{channelId}/tasks/pull?nodeId={nodeId}&limit=10`
- `POST /api/claw/task-items/{taskItemId}/events`
- `POST /api/claw/task-items/{taskItemId}/finish`

## Request Design Notes

Request bodies use concrete DTO classes. Task and report payloads use Jackson polymorphic binding from the request `type`, so the controller layer does not accept catch-all `JSONObject`, `JsonNode`, or `Map` request bodies.

Task counters are not stored in `tasks`; they are derived from `task_items` in query responses.
