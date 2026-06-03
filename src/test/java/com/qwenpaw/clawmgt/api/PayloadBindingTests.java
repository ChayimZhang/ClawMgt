package com.qwenpaw.clawmgt.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qwenpaw.clawmgt.api.dto.request.CreateTaskRequest;
import com.qwenpaw.clawmgt.api.dto.request.ReportDataRequest;
import com.qwenpaw.clawmgt.api.payload.report.SkillMetadataReportPayload;
import com.qwenpaw.clawmgt.api.payload.task.ChatTaskPayload;
import com.qwenpaw.clawmgt.api.payload.task.SkillInstallPayload;
import com.qwenpaw.clawmgt.api.payload.task.SkillRemovePayload;
import com.qwenpaw.clawmgt.api.payload.task.SkillUpgradePayload;
import com.qwenpaw.clawmgt.api.payload.task.ParamUpdatePayload;
import com.qwenpaw.clawmgt.domain.enums.ReportType;
import com.qwenpaw.clawmgt.domain.enums.TaskType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PayloadBindingTests {
    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    Validator validator;

    @Test
    void bindsSkillInstallTaskPayloadFromTaskType() throws Exception {
        CreateTaskRequest request = objectMapper.readValue("""
                {
                  "channelId": 1,
                  "type": "skill_install",
                  "title": "Install skills",
                  "targetNodeIds": [10, 11],
                  "payload": {
                    "force": true,
                    "skills": [
                      {
                        "skillName": "browser",
                        "version": "1.2.3",
                        "downloadUrl": "https://example.com/browser-1.2.3.zip",
                        "parameters": {
                          "timeoutSeconds": 30,
                          "enabled": true
                        }
                      }
                    ]
                  }
                }
                """, CreateTaskRequest.class);

        assertThat(request.getPayload()).isInstanceOf(SkillInstallPayload.class);
        assertThat(request.getType()).isEqualTo(TaskType.SKILL_INSTALL);
        SkillInstallPayload payload = (SkillInstallPayload) request.getPayload();
        assertThat(payload.getSkills()).hasSize(1);
        assertThat(payload.getSkills().getFirst().getSkillName()).isEqualTo("browser");
        assertThat(payload.isForce()).isTrue();
    }

    @Test
    void bindsChatTaskPayloadFromTaskType() throws Exception {
        CreateTaskRequest request = objectMapper.readValue("""
                {
                  "channelId": 1,
                  "type": "chat",
                  "title": "Ask node",
                  "payload": {
                    "sessionId": 42,
                    "content": "hello",
                    "title": "Greeting"
                  }
                }
                """, CreateTaskRequest.class);

        assertThat(request.getPayload()).isInstanceOf(ChatTaskPayload.class);
        assertThat(request.getType()).isEqualTo(TaskType.CHAT);
        ChatTaskPayload payload = (ChatTaskPayload) request.getPayload();
        assertThat(payload.getSessionId()).isEqualTo(42L);
        assertThat(payload.getContent()).isEqualTo("hello");
    }

    @Test
    void bindsSkillUpgradeTaskPayloadFromTaskType() throws Exception {
        CreateTaskRequest request = objectMapper.readValue("""
                {
                  "channelId": 1,
                  "type": "skill_upgrade",
                  "title": "Upgrade skills",
                  "payload": {
                    "force": false,
                    "skills": [
                      {
                        "skillName": "browser",
                        "version": "1.2.4",
                        "downloadUrl": "http://example.com/browser-1.2.4.zip"
                      }
                    ]
                  }
                }
                """, CreateTaskRequest.class);

        assertThat(request.getType()).isEqualTo(TaskType.SKILL_UPGRADE);
        assertThat(request.getPayload()).isInstanceOf(SkillUpgradePayload.class);
        SkillUpgradePayload payload = (SkillUpgradePayload) request.getPayload();
        assertThat(payload.getSkills()).hasSize(1);
        assertThat(payload.getSkills().getFirst().getVersion()).isEqualTo("1.2.4");
    }

    @Test
    void bindsSkillRemoveTaskPayloadFromTaskType() throws Exception {
        CreateTaskRequest request = objectMapper.readValue("""
                {
                  "channelId": 1,
                  "type": "skill_remove",
                  "title": "Remove skills",
                  "payload": {
                    "skillNames": ["browser"]
                  }
                }
                """, CreateTaskRequest.class);

        assertThat(request.getType()).isEqualTo(TaskType.SKILL_REMOVE);
        assertThat(request.getPayload()).isInstanceOf(SkillRemovePayload.class);
        SkillRemovePayload payload = (SkillRemovePayload) request.getPayload();
        assertThat(payload.getSkillNames()).containsExactly("browser");
    }

    @Test
    void bindsParamUpdateTaskPayloadFromTaskType() throws Exception {
        CreateTaskRequest request = objectMapper.readValue("""
                {
                  "channelId": 1,
                  "type": "param_update",
                  "title": "Update params",
                  "payload": {
                    "skillName": "browser",
                    "parameters": {
                      "timeoutSeconds": 60
                    }
                  }
                }
                """, CreateTaskRequest.class);

        assertThat(request.getType()).isEqualTo(TaskType.PARAM_UPDATE);
        assertThat(request.getPayload()).isInstanceOf(ParamUpdatePayload.class);
        ParamUpdatePayload payload = (ParamUpdatePayload) request.getPayload();
        assertThat(payload.getParameters()).containsEntry("timeoutSeconds", 60);
    }

    @Test
    void bindsSkillMetadataReportPayloadFromReportType() throws Exception {
        ReportDataRequest request = objectMapper.readValue("""
                {
                  "type": "skill_metadata",
                  "payload": {
                    "skills": [
                      {
                        "skillName": "browser",
                        "version": "1.2.3",
                        "parameters": {
                          "timeoutSeconds": 30
                        }
                      }
                    ]
                  }
                }
                """, ReportDataRequest.class);

        assertThat(request.getType()).isEqualTo(ReportType.SKILL_METADATA);
        assertThat(request.getPayload()).isInstanceOf(SkillMetadataReportPayload.class);
        SkillMetadataReportPayload payload = (SkillMetadataReportPayload) request.getPayload();
        assertThat(payload.getSkills()).hasSize(1);
        assertThat(payload.getSkills().getFirst().getSkillName()).isEqualTo("browser");
    }

    @Test
    void malformedTaskPayloadsFailBeanValidation() throws Exception {
        CreateTaskRequest request = objectMapper.readValue("""
                {
                  "channelId": 1,
                  "type": "skill_install",
                  "title": "Install skills",
                  "targetNodeIds": [],
                  "payload": {
                    "skills": [
                      {
                        "skillName": "",
                        "version": "",
                        "downloadUrl": "not a uri"
                      }
                    ]
                  }
                }
                """, CreateTaskRequest.class);

        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains(
                        "targetNodeIds",
                        "payload.skills[0].skillName",
                        "payload.skills[0].version",
                        "payload.skills[0].downloadUrl"
                );
    }

    @Test
    void malformedReportPayloadsFailBeanValidation() throws Exception {
        ReportDataRequest request = objectMapper.readValue("""
                {
                  "type": "skill_metadata",
                  "payload": {
                    "skills": []
                  }
                }
                """, ReportDataRequest.class);

        Set<ConstraintViolation<ReportDataRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("payload.skills");
    }
}
