package com.example.food.agent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentToolRegistryTest {

    private final AgentToolRegistry registry = new AgentToolRegistry();

    @Test
    void onlyRegisteredToolsCanBeResolved() {
        assertThat(registry.require("PANTRY.LIST"))
                .isEqualTo(AgentToolRegistry.Tool.PANTRY_LIST);
        assertThat(registry.require("nutrition.profile"))
                .isEqualTo(AgentToolRegistry.Tool.NUTRITION_PROFILE);
        assertThat(registry.requireFunction("recipe_generate"))
                .isEqualTo(AgentToolRegistry.Tool.RECIPE_GENERATE);
        assertThat(registry.requireFunction("recipe_save"))
                .isEqualTo(AgentToolRegistry.Tool.RECIPE_SAVE);
        java.util.List<String> functionNames = registry.functionDefinitions().stream()
                .map(definition -> ((java.util.Map<?, ?>) definition.get("function")).get("name").toString())
                .toList();
        assertThat(functionNames)
                .contains("pantry_list", "recipe_generate", "recipe_save")
                .allSatisfy(name -> assertThat(name).doesNotContain("."));
    }

    @Test
    void ordinaryChatDoesNotExposeKitchenTools() {
        assertThat(registry.functionDefinitions("你好，讲个笑话", false)).isEmpty();
    }

    @Test
    void selectsOnlyRelevantDomainTools() {
        java.util.List<String> dateTools = registry.functionDefinitions("今天几号", false).stream()
                .map(definition -> ((java.util.Map<?, ?>) definition.get("function")).get("name").toString())
                .toList();
        java.util.List<String> pantryTools = registry.functionDefinitions("帮我消耗两个鸡蛋", false).stream()
                .map(definition -> ((java.util.Map<?, ?>) definition.get("function")).get("name").toString())
                .toList();

        assertThat(dateTools).containsExactly("current_datetime");
        assertThat(pantryTools).contains("pantry_list", "pantry_expiry", "pantry_manage")
                .doesNotContain("notification_manage", "profile_manage");
    }

    @Test
    void rejectsUnregisteredOrBlankTools() {
        assertThatThrownBy(() -> registry.require("admin.users"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.require("  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.require(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.requireFunction("admin_users"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
