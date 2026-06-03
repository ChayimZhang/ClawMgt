package com.qwenpaw.clawmgt.config;

import com.qwenpaw.clawmgt.domain.enums.TaskCategory;
import com.qwenpaw.clawmgt.domain.enums.TaskType;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@ConfigurationProperties(prefix = "clawmgt.tasks.concurrency")
public class TaskConcurrencyProperties {
    private Defaults defaults = new Defaults();
    private Map<String, TypeConcurrency> types = new HashMap<>();

    @PostConstruct
    void validate() {
        if (defaults == null) {
            defaults = new Defaults();
        }
        if (types == null) {
            types = new HashMap<>();
        }
        types.values().forEach(type -> {
            if (type.mode == null) {
                throw new IllegalStateException("Task concurrency mode must be configured when a type entry exists");
            }
        });
    }

    public Policy policyFor(TaskType taskType, TaskCategory category) {
        TypeConcurrency type = types.get(normalize(taskType.name()));
        if (type == null) {
            type = types.get(taskType.name().toLowerCase(Locale.ROOT));
        }
        if (type != null) {
            return new Policy(blankToDefault(type.group, defaultGroup(taskType)), type.mode);
        }
        if (taskType == TaskType.CHAT || category == TaskCategory.CHAT) {
            return new Policy("chat", ConcurrencyMode.PARALLEL);
        }
        return new Policy(defaultGroup(taskType), defaults.lifecycleMode);
    }

    private String defaultGroup(TaskType taskType) {
        return taskType.name().toLowerCase(Locale.ROOT);
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace("_", "-");
    }

    public Defaults getDefaults() {
        return defaults;
    }

    public void setDefaults(Defaults defaults) {
        this.defaults = defaults;
    }

    public Map<String, TypeConcurrency> getTypes() {
        return types;
    }

    public void setTypes(Map<String, TypeConcurrency> types) {
        this.types = types;
    }

    public static class Defaults {
        private ConcurrencyMode lifecycleMode = ConcurrencyMode.MUTEX_GROUP;

        public ConcurrencyMode getLifecycleMode() {
            return lifecycleMode;
        }

        public void setLifecycleMode(ConcurrencyMode lifecycleMode) {
            this.lifecycleMode = lifecycleMode;
        }
    }

    public static class TypeConcurrency {
        private String group;
        private ConcurrencyMode mode;

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public ConcurrencyMode getMode() {
            return mode;
        }

        public void setMode(ConcurrencyMode mode) {
            this.mode = mode;
        }
    }

    public enum ConcurrencyMode {
        PARALLEL,
        MUTEX_GROUP,
        EXCLUSIVE_NODE
    }

    public record Policy(String group, ConcurrencyMode mode) {
    }
}
