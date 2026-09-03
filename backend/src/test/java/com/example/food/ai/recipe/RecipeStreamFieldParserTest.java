package com.example.food.ai.recipe;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeStreamFieldParserTest {

    private final RecipeStreamFieldParser parser = new RecipeStreamFieldParser(new ObjectMapper());

    @Test
    void emitsOnlyCompleteTopLevelFieldsAndOnlyOnce() {
        assertThat(parser.accept("{\"title\":\"番")).isEmpty();

        Map<String, com.fasterxml.jackson.databind.JsonNode> first = parser.accept(
                "茄炒蛋\",\"summary\":\"家常快手菜\",\"ingredients\":["
        );

        assertThat(first).containsKeys("title", "summary");
        assertThat(first.get("title").asText()).isEqualTo("番茄炒蛋");
        assertThat(first.get("summary").asText()).isEqualTo("家常快手菜");
        assertThat(parser.accept("{\"name\":\"番茄\",\"amount\":\"2个\"} ]"))
                .containsKey("ingredients");
        assertThat(parser.accept(",\"steps\":[{\"order\":1,\"title\":\"炒制\",\"description\":\"炒熟\"}]}"))
                .containsKey("steps");
        assertThat(parser.accept("")).isEmpty();
    }
}
