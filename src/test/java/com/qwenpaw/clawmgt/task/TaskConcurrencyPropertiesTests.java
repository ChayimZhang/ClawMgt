package com.qwenpaw.clawmgt.task;

import com.qwenpaw.clawmgt.config.TaskConcurrencyProperties;
import com.qwenpaw.clawmgt.config.TaskConcurrencyProperties.ConcurrencyMode;
import com.qwenpaw.clawmgt.domain.enums.TaskType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskConcurrencyPropertiesTests {
    @Test
    void chatDoesNotHaveHardCodedParallelPolicy() {
        TaskConcurrencyProperties properties = new TaskConcurrencyProperties();

        assertThat(properties.policyFor(TaskType.CHAT).mode()).isEqualTo(ConcurrencyMode.MUTEX_GROUP);
    }
}
