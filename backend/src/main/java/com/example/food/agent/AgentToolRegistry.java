package com.example.food.agent;

import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

@Component
public class AgentToolRegistry {

    private final Set<Tool> allowedTools = EnumSet.allOf(Tool.class);

    public Tool require(String name) {
        if (name == null) {
            throw new IllegalArgumentException("工具名称不能为空");
        }
        for (Tool tool : allowedTools) {
            if (tool.toolName.equals(name.trim().toLowerCase(Locale.ROOT))) {
                return tool;
            }
        }
        throw new IllegalArgumentException("未注册的厨房助手工具");
    }

    public Tool requireFunction(String functionName) {
        if (functionName == null) {
            throw new IllegalArgumentException("工具函数名称不能为空");
        }
        for (Tool tool : allowedTools) {
            if (tool.functionName.equals(functionName.trim().toLowerCase(Locale.ROOT))) {
                return tool;
            }
        }
        throw new IllegalArgumentException("模型请求了未注册的厨房助手工具");
    }

    public List<Map<String, Object>> functionDefinitions() {
        return allowedTools.stream().map(Tool::functionDefinition).toList();
    }

    public enum Tool {
        PANTRY_LIST(
                "pantry.list",
                "pantry_list",
                "小仓正在读取食材库存",
                "查询当前登录用户的真实食材库存、数量、单位与到期状态。"
        ),
        PANTRY_EXPIRY(
                "pantry.expiry",
                "pantry_expiry",
                "小仓正在检查临期食材",
                "查询当前登录用户已经过期和即将过期的食材。"
        ),
        NOTIFICATIONS(
                "notifications.list",
                "notifications_list",
                "小厨灵正在读取提醒",
                "查询当前登录用户最近的未读提醒。"
        ),
        WEEKLY_MENU(
                "weekly-menu.read",
                "weekly_menu_read",
                "周周正在读取本周菜单",
                "查询当前登录用户本周菜单和购物清单。"
        ),
        SAVED_RECIPES(
                "recipes.saved",
                "recipes_saved",
                "阿灶正在读取已保存菜谱",
                "查询当前登录用户最近保存的菜谱。"
        ),
        NUTRITION_PROFILE(
                "nutrition.profile",
                "nutrition_profile",
                "小厨灵正在读取营养目标",
                "查询当前登录用户的健康档案、饮食偏好和每日营养目标。"
        ),
        RECIPE_GENERATE(
                "recipe.generate",
                "recipe_generate",
                "阿灶正在生成菜谱",
                "根据用户要求、真实库存、临期食材和饮食偏好生成一道菜谱。"
        ),
        RECIPE_SAVE(
                "recipe.save",
                "recipe_save",
                "小厨灵正在准备保存菜谱",
                "请求保存本次会话最近生成的菜谱。只会发起用户确认，不会直接写入。"
        );

        private final String toolName;
        private final String functionName;
        private final String label;
        private final String description;

        Tool(String toolName, String functionName, String label, String description) {
            this.toolName = toolName;
            this.functionName = functionName;
            this.label = label;
            this.description = description;
        }

        public String toolName() {
            return toolName;
        }

        public String label() {
            return label;
        }

        public String functionName() {
            return functionName;
        }

        private Map<String, Object> functionDefinition() {
            Map<String, Object> parameters = this == RECIPE_GENERATE
                    ? Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "request", Map.of(
                                    "type", "string",
                                    "description", "用户对食材、餐次、口味和菜谱的要求"
                            ),
                            "meal_type", Map.of(
                                    "type", "string",
                                    "enum", List.of("breakfast", "lunch", "dinner"),
                                    "description", "餐次；不确定时可省略"
                            ),
                            "prefer_expiring", Map.of(
                                    "type", "boolean",
                                    "description", "是否优先使用临期食材"
                            )
                    ),
                    "required", List.of("request"),
                    "additionalProperties", false
            )
                    : Map.of(
                    "type", "object",
                    "properties", Map.of(),
                    "additionalProperties", false
            );
            return Map.of(
                    "type", "function",
                    "function", Map.of(
                            "name", functionName,
                            "description", description,
                            "parameters", parameters
                    )
            );
        }
    }
}
