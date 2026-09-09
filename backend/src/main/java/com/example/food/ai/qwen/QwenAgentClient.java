package com.example.food.ai.qwen;

import com.example.food.ai.config.AiModelConfigService;
import com.example.food.ai.config.AiModelRuntimeConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class QwenAgentClient {

    private static final String SYSTEM_PROMPT = """
            你是“小厨灵”，一个中文家庭厨房智能助手。
            你的职责是理解用户目标，自主选择已提供的工具，并根据真实工具结果回答。

            必须遵守：
            1. 查询库存、临期、提醒、周菜单、收藏或营养信息时必须调用对应工具，不得编造。
            2. 用户要求根据食材生成、推荐或调整菜谱时，调用 recipe_generate。
            3. 用户要求保存最近生成的菜谱时，调用 recipe_save；该工具只发起用户确认，不会直接写入。
            4. 只能调用 tools 中声明的函数，工具参数必须符合 JSON Schema。
            5. 工具返回错误时如实说明，不要假装执行成功。
            6. 回答简洁、友好，使用中文；营养内容仅作一般饮食参考，不作医疗判断。
            """;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final QwenProperties properties;
    private final AiModelConfigService aiModelConfigService;

    @Autowired
    public QwenAgentClient(
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper,
            QwenProperties properties,
            AiModelConfigService aiModelConfigService
    ) {
        this(
                restTemplateBuilder
                        .setConnectTimeout(Duration.ofSeconds(5))
                        .setReadTimeout(Duration.ofSeconds(120))
                        .build(),
                objectMapper,
                properties,
                aiModelConfigService
        );
    }

    public QwenAgentClient(RestTemplate restTemplate, ObjectMapper objectMapper, QwenProperties properties) {
        this(restTemplate, objectMapper, properties, null);
    }

    public QwenAgentClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            QwenProperties properties,
            AiModelConfigService aiModelConfigService
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.aiModelConfigService = aiModelConfigService;
    }

    public AgentTurn complete(List<ConversationMessage> conversation, List<Map<String, Object>> tools) {
        AiModelRuntimeConfig runtimeConfig = runtimeConfig();
        if (runtimeConfig.apiKey() == null || runtimeConfig.apiKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "千问 API Key 未配置，请设置 DASHSCOPE_API_KEY");
        }

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    runtimeConfig.endpoint(),
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody(conversation, tools, runtimeConfig), headers(runtimeConfig)),
                    JsonNode.class
            );
            return parse(response.getBody(), runtimeConfig);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问 Agent 服务调用失败，请稍后重试", exception);
        }
    }

    private AiModelRuntimeConfig runtimeConfig() {
        if (aiModelConfigService != null) {
            return aiModelConfigService.textRecipeRuntimeConfig();
        }
        return new AiModelRuntimeConfig("qwen", properties.model(), properties.endpoint(), properties.apiKey());
    }

    private Map<String, Object> requestBody(
            List<ConversationMessage> conversation,
            List<Map<String, Object>> tools,
            AiModelRuntimeConfig runtimeConfig
    ) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        if (conversation != null) {
            conversation.forEach(message -> messages.add(message.toPayload()));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", runtimeConfig.modelName());
        body.put("messages", messages);
        body.put("tools", tools == null ? List.of() : tools);
        body.put("tool_choice", "auto");
        body.put("parallel_tool_calls", false);
        body.put("temperature", 0.2);
        return body;
    }

    private HttpHeaders headers(AiModelRuntimeConfig runtimeConfig) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(runtimeConfig.apiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private AgentTurn parse(JsonNode root, AiModelRuntimeConfig runtimeConfig) {
        JsonNode choices = root == null ? null : root.path("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            choices = root == null ? null : root.path("output").path("choices");
        }
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问 Agent 服务未返回有效结果");
        }

        JsonNode message = choices.get(0).path("message");
        String content = message.path("content").isTextual() ? message.path("content").textValue().trim() : "";
        List<ToolCall> toolCalls = new ArrayList<>();
        JsonNode calls = message.path("tool_calls");
        if (calls.isArray()) {
            for (JsonNode call : calls) {
                JsonNode function = call.path("function");
                String id = call.path("id").asText("").trim();
                String name = function.path("name").asText("").trim();
                String arguments = function.path("arguments").asText("{}");
                if (id.isEmpty() || name.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问 Agent 返回了无效工具调用");
                }
                toolCalls.add(new ToolCall(id, name, arguments));
            }
        }
        if (content.isEmpty() && toolCalls.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "千问 Agent 没有返回回答或工具调用");
        }
        return new AgentTurn(content, List.copyOf(toolCalls), runtimeConfig.provider(), runtimeConfig.modelName());
    }

    public record AgentTurn(String content, List<ToolCall> toolCalls, String provider, String model) {
    }

    public record ToolCall(String id, String name, String arguments) {
    }

    public record ConversationMessage(
            String role,
            String content,
            List<ToolCall> toolCalls,
            String toolCallId
    ) {
        public static ConversationMessage user(String content) {
            return new ConversationMessage("user", content, List.of(), null);
        }

        public static ConversationMessage assistant(String content) {
            return new ConversationMessage("assistant", content, List.of(), null);
        }

        public static ConversationMessage assistant(AgentTurn turn) {
            return new ConversationMessage("assistant", turn.content(), turn.toolCalls(), null);
        }

        public static ConversationMessage tool(String toolCallId, String content) {
            return new ConversationMessage("tool", content, List.of(), toolCallId);
        }

        private Map<String, Object> toPayload() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("role", role);
            if ("tool".equals(role)) {
                payload.put("tool_call_id", toolCallId);
                payload.put("content", content == null ? "" : content);
                return payload;
            }
            payload.put("content", content == null ? "" : content);
            if (toolCalls != null && !toolCalls.isEmpty()) {
                payload.put("tool_calls", toolCalls.stream().map(call -> Map.of(
                        "id", call.id(),
                        "type", "function",
                        "function", Map.of(
                                "name", call.name(),
                                "arguments", call.arguments()
                        )
                )).toList());
            }
            return payload;
        }
    }
}
