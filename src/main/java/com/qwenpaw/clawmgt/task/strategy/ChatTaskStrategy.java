package com.qwenpaw.clawmgt.task.strategy;

import com.qwenpaw.clawmgt.api.payload.task.ChatTaskPayload;
import com.qwenpaw.clawmgt.domain.entity.NodeEntity;
import com.qwenpaw.clawmgt.domain.enums.TaskCategory;
import com.qwenpaw.clawmgt.domain.enums.TaskStatus;
import com.qwenpaw.clawmgt.domain.enums.TaskType;
import com.qwenpaw.clawmgt.task.TaskStrategy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatTaskStrategy implements TaskStrategy<ChatTaskPayload> {
    @Override
    public TaskType taskType() {
        return TaskType.CHAT;
    }

    @Override
    public TaskCategory category() {
        return TaskCategory.CHAT;
    }

    @Override
    public Class<ChatTaskPayload> payloadClass() {
        return ChatTaskPayload.class;
    }

    @Override
    public NodeDispatchPlan buildTypedDispatchPlan(ChatTaskPayload payload, NodeEntity node) {
        return new NodeDispatchPlan(TaskStatus.PENDING, payload, List.of(), null);
    }
}
