package com.example.food.ai.qwen;

import com.example.food.agent.AgentToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class QwenAgentClientTest {

    @Test
    void sendsRegisteredToolsAndParsesToolCall() {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        QwenProperties properties = properties("test-api-key");
        QwenAgentClient client = new QwenAgentClient(restTemplate, new ObjectMapper(), properties);
        AgentToolRegistry registry = new AgentToolRegistry();

        server.expect(once(), requestTo(properties.endpoint()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(jsonPath("$.model").value("qwen-plus"))
                .andExpect(jsonPath("$.tool_choice").value("auto"))
                .andExpect(jsonPath("$.parallel_tool_calls").value(false))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content").value("我的库存有什么？"))
                .andExpect(jsonPath("$.tools[0].type").value("function"))
                .andRespond(withSuccess("""
                        {
                          "choices": [{
                            "message": {
                              "content": "",
                              "tool_calls": [{
                                "id": "call_inventory_1",
                                "type": "function",
                                "function": {"name": "pantry_list", "arguments": "{}"}
                              }]
                            }
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        QwenAgentClient.AgentTurn turn = client.complete(
                List.of(QwenAgentClient.ConversationMessage.user("我的库存有什么？")),
                registry.functionDefinitions()
        );

        assertThat(turn.content()).isEmpty();
        assertThat(turn.provider()).isEqualTo("qwen");
        assertThat(turn.model()).isEqualTo("qwen-plus");
        assertThat(turn.toolCalls()).containsExactly(
                new QwenAgentClient.ToolCall("call_inventory_1", "pantry_list", "{}")
        );
        server.verify();
    }

    @Test
    void sendsToolResultBackAndParsesFinalAnswer() {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        QwenProperties properties = properties("test-api-key");
        QwenAgentClient client = new QwenAgentClient(restTemplate, new ObjectMapper(), properties);
        QwenAgentClient.ToolCall call = new QwenAgentClient.ToolCall("call_1", "pantry_list", "{}");

        server.expect(once(), requestTo(properties.endpoint()))
                .andExpect(jsonPath("$.messages[2].tool_calls[0].id").value("call_1"))
                .andExpect(jsonPath("$.messages[3].role").value("tool"))
                .andExpect(jsonPath("$.messages[3].tool_call_id").value("call_1"))
                .andExpect(jsonPath("$.messages[3].content").value("{\"count\":2}"))
                .andRespond(withSuccess("""
                        {
                          "choices": [{
                            "message": {"content": "你目前有 2 种食材。"}
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        QwenAgentClient.AgentTurn turn = client.complete(
                List.of(
                        QwenAgentClient.ConversationMessage.user("我的库存有什么？"),
                        QwenAgentClient.ConversationMessage.assistant(
                                new QwenAgentClient.AgentTurn("", List.of(call), "qwen", "qwen-plus")
                        ),
                        QwenAgentClient.ConversationMessage.tool("call_1", "{\"count\":2}")
                ),
                new AgentToolRegistry().functionDefinitions()
        );

        assertThat(turn.content()).isEqualTo("你目前有 2 种食材。");
        assertThat(turn.toolCalls()).isEmpty();
        server.verify();
    }

    @Test
    void refusesCallWithoutApiKey() {
        QwenAgentClient client = new QwenAgentClient(
                new RestTemplateBuilder().build(),
                new ObjectMapper(),
                properties("")
        );

        assertThatThrownBy(() -> client.complete(
                List.of(QwenAgentClient.ConversationMessage.user("你好")),
                new AgentToolRegistry().functionDefinitions()
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("DASHSCOPE_API_KEY");
    }

    private QwenProperties properties(String apiKey) {
        return new QwenProperties(
                apiKey,
                "qwen-plus",
                "https://dashscope.test/compatible-mode/v1/chat/completions"
        );
    }
}
