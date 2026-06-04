package com.qwenpaw.clawmgt.api;

import com.qwenpaw.clawmgt.domain.repository.ChannelRepository;
import com.qwenpaw.clawmgt.domain.repository.MessageRepository;
import com.qwenpaw.clawmgt.domain.repository.NodeReportRepository;
import com.qwenpaw.clawmgt.domain.repository.NodeRepository;
import com.qwenpaw.clawmgt.domain.repository.NodeSkillMetadataRepository;
import com.qwenpaw.clawmgt.domain.repository.SessionRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskEventRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskItemDetailRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskItemRepository;
import com.qwenpaw.clawmgt.domain.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ControllerIntegrationTests {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ChannelRepository channelRepository;
    @Autowired
    NodeRepository nodeRepository;
    @Autowired
    NodeReportRepository nodeReportRepository;
    @Autowired
    NodeSkillMetadataRepository nodeSkillMetadataRepository;
    @Autowired
    TaskRepository taskRepository;
    @Autowired
    TaskItemRepository taskItemRepository;
    @Autowired
    TaskItemDetailRepository taskItemDetailRepository;
    @Autowired
    TaskEventRepository taskEventRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    MessageRepository messageRepository;

    @BeforeEach
    void cleanDatabase() {
        messageRepository.deleteAll();
        taskEventRepository.deleteAll();
        taskItemDetailRepository.deleteAll();
        taskItemRepository.deleteAll();
        taskRepository.deleteAll();
        sessionRepository.deleteAll();
        nodeSkillMetadataRepository.deleteAll();
        nodeReportRepository.deleteAll();
        nodeRepository.deleteAll();
        channelRepository.deleteAll();
    }

    @Test
    void nodeReportsTasksAndEventsUseConcreteApiRequests() throws Exception {
        long channelId = createChannel();
        long nodeId = registerNode(channelId, "edge-1");

        mockMvc.perform(post("/api/claw/nodes/{nodeId}/heartbeat", nodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(nodeId));

        mockMvc.perform(post("/api/claw/nodes/{nodeId}/reports", nodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "skill_metadata",
                                  "payload": {
                                    "skills": [
                                      {
                                        "skillName": "browser",
                                        "version": "1.0.0",
                                        "parameters": {"enabled": true}
                                      }
                                    ]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPLIED"));
        assertThat(nodeReportRepository.findAll()).hasSize(1);
        assertThat(nodeSkillMetadataRepository.findByNodeIdAndSkillName(nodeId, "browser")).isPresent();

        long taskId = createSkillInstallTask(channelId);
        long taskItemId = taskItemRepository.findByTaskIdOrderByIdAsc(taskId).getFirst().getId();

        mockMvc.perform(get("/api/tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counts.total").value(1))
                .andExpect(jsonPath("$.data.items[0].details[0].detailKey").value("browser"));

        mockMvc.perform(get("/api/claw/channels/{channelId}/tasks/pull", channelId)
                        .param("nodeId", String.valueOf(nodeId))
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(taskItemId))
                .andExpect(jsonPath("$.data[0].status").value("PULLED"));

        mockMvc.perform(post("/api/claw/task-items/{taskItemId}/events", taskItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "evt-1",
                                  "eventType": "progress",
                                  "status": "RUNNING",
                                  "content": "installing"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eventId").value("evt-1"));

        mockMvc.perform(post("/api/claw/task-items/{taskItemId}/finish", taskItemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "SUCCEEDED",
                                  "result": "ok"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
    }

    @Test
    void conversationMessageCreatesChannelScopedChatTask() throws Exception {
        long channelId = createChannel();
        registerNode(channelId, "edge-1");

        MvcResult sessionResult = mockMvc.perform(post("/api/channels/{channelId}/sessions", channelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Support chat"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.channelId").value(channelId))
                .andReturn();
        long sessionId = idFrom(sessionResult);

        mockMvc.perform(post("/api/sessions/{sessionId}/messages", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "hello"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("hello"))
                .andExpect(jsonPath("$.data.taskId").isNumber());

        mockMvc.perform(get("/api/sessions/{sessionId}/messages", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("hello"));

        assertThat(taskRepository.findAll()).hasSize(1);
        assertThat(taskItemRepository.findAll()).hasSize(1);
    }

    @Test
    void requestValidationRejectsInvalidConcreteBody() throws Exception {
        mockMvc.perform(post("/api/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "missing name"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private long createChannel() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/channels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "alpha",
                                  "description": "test channel"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return idFrom(result);
    }

    private long registerNode(long channelId, String nodeKey) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/claw/channels/{channelId}/nodes/register", channelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nodeKey": "%s",
                                  "hostname": "edge-host",
                                  "ipAddress": "127.0.0.1",
                                  "clawVersion": "1.0.0"
                                }
                                """.formatted(nodeKey)))
                .andExpect(status().isOk())
                .andReturn();
        return idFrom(result);
    }

    private long createSkillInstallTask(long channelId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "channelId": %d,
                                  "type": "skill_install",
                                  "title": "Install browser",
                                  "payload": {
                                    "skills": [
                                      {
                                        "skillName": "browser",
                                        "version": "1.2.0",
                                        "downloadUrl": "https://example.test/browser.zip"
                                      }
                                    ],
                                    "force": false
                                  }
                                }
                                """.formatted(channelId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.counts.pending").value(1))
                .andReturn();
        return idFrom(result);
    }

    private long idFrom(MvcResult result) throws Exception {
        Number id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return id.longValue();
    }
}
