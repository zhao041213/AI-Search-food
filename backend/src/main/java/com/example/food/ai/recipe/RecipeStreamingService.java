package com.example.food.ai.recipe;

import com.example.food.ai.qwen.QwenRecipeClient;
import com.example.food.ai.recipe.dto.RecipeGenerateRequest;
import com.example.food.ai.recipe.dto.RecipeGenerateResponse;
import com.example.food.security.AuthPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class RecipeStreamingService {

    private static final long STREAM_TIMEOUT_MILLIS = 120_000L;

    private final RecipeRecommendationService recipeRecommendationService;
    private final QwenRecipeClient qwenRecipeClient;
    private final ExecutorService workerExecutor = Executors.newCachedThreadPool(daemonThreadFactory("recipe-stream"));
    private final ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(
            1,
            daemonThreadFactory("recipe-heartbeat")
    );

    public RecipeStreamingService(
            RecipeRecommendationService recipeRecommendationService,
            QwenRecipeClient qwenRecipeClient
    ) {
        this.recipeRecommendationService = recipeRecommendationService;
        this.qwenRecipeClient = qwenRecipeClient;
    }

    public SseEmitter generate(
            RecipeGenerateRequest request,
            AuthPrincipal principal,
            String anonymousId
    ) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicReference<Future<?>> worker = new AtomicReference<>();
        AtomicReference<ScheduledFuture<?>> heartbeat = new AtomicReference<>();
        Runnable cancel = () -> {
            cancelled.set(true);
            Future<?> currentWorker = worker.get();
            if (currentWorker != null) {
                currentWorker.cancel(true);
            }
            ScheduledFuture<?> currentHeartbeat = heartbeat.get();
            if (currentHeartbeat != null) {
                currentHeartbeat.cancel(false);
            }
        };
        emitter.onCompletion(cancel);
        emitter.onTimeout(cancel);
        emitter.onError(error -> cancel.run());

        if (!sendStatus(emitter, cancelled, "preparing", "正在准备菜谱生成")) {
            cancel.run();
            return emitter;
        }

        ScheduledFuture<?> heartbeatTask = heartbeatExecutor.scheduleAtFixedRate(
                () -> sendHeartbeat(emitter, cancelled),
                15,
                15,
                TimeUnit.SECONDS
        );
        heartbeat.set(heartbeatTask);

        Future<?> task = workerExecutor.submit(() -> runGeneration(
                emitter,
                cancelled,
                request,
                principal,
                anonymousId,
                heartbeatTask
        ));
        worker.set(task);
        if (cancelled.get()) {
            task.cancel(true);
        }
        return emitter;
    }

    private void runGeneration(
            SseEmitter emitter,
            AtomicBoolean cancelled,
            RecipeGenerateRequest request,
            AuthPrincipal principal,
            String anonymousId,
            ScheduledFuture<?> heartbeat
    ) {
        try {
            requireActive(cancelled);
            String prompt = recipeRecommendationService.promptFor(request, principal);
            requireActive(cancelled);
            sendStatusOrCancel(emitter, cancelled, "generating", "正在连接 AI 生成服务");

            RecipeStreamFieldParser parser = new RecipeStreamFieldParser(new com.fasterxml.jackson.databind.ObjectMapper());
            QwenRecipeClient.RecipeStreamResult streamResult = qwenRecipeClient.streamRecipe(
                    prompt,
                    delta -> {
                        requireActive(cancelled);
                        Map<String, JsonNode> fields = parser.accept(delta);
                        sendFieldGroups(emitter, cancelled, fields);
                    },
                    () -> sendStatusOrCancel(emitter, cancelled, "receiving", "AI 正在生成菜谱内容")
            );
            requireActive(cancelled);
            sendStatusOrCancel(emitter, cancelled, "parsing", "正在整理菜谱内容");

            RecipeGenerateResponse response = streamResult.fallbackResponse();
            if (response == null) {
                response = qwenRecipeClient.parseRecipeContent(
                        streamResult.content(),
                        qwenRecipeClient.currentRuntimeConfig()
                );
            }
            validateRecipe(response);
            requireActive(cancelled);
            sendStatusOrCancel(emitter, cancelled, "saving", "正在保存本次搜索记录");
            requireActive(cancelled);
            RecipeGenerateResponse persisted = recipeRecommendationService.persist(
                    request,
                    response,
                    principal,
                    anonymousId
            );
            requireActive(cancelled);
            sendOrCancel(emitter, cancelled, "complete", persisted);
            emitter.complete();
        } catch (StreamCancelledException | java.util.concurrent.CancellationException exception) {
            // The client has left or a newer request has superseded this stream.
        } catch (Throwable exception) {
            if (cancelled.get() || Thread.currentThread().isInterrupted()) {
                return;
            }
            sendError(emitter, cancelled, errorMessage(exception));
            emitter.complete();
        } finally {
            heartbeat.cancel(false);
        }
    }

    private void sendFieldGroups(
            SseEmitter emitter,
            AtomicBoolean cancelled,
            Map<String, JsonNode> fields
    ) {
        if (fields.isEmpty()) {
            return;
        }
        sendGroup(emitter, cancelled, "overview", fields, "title", "summary", "effects");
        sendGroup(emitter, cancelled, "ingredients", fields, "ingredients", "missingIngredients");
        sendGroup(emitter, cancelled, "steps", fields, "steps", "tips", "videoKeywords");
        sendGroup(emitter, cancelled, "details", fields, "explanation", "nutritionEstimate");
    }

    private void sendGroup(
            SseEmitter emitter,
            AtomicBoolean cancelled,
            String event,
            Map<String, JsonNode> fields,
            String... fieldNames
    ) {
        Map<String, JsonNode> group = new LinkedHashMap<>();
        for (String fieldName : fieldNames) {
            JsonNode value = fields.get(fieldName);
            if (value != null) {
                group.put(fieldName, value);
            }
        }
        if (!group.isEmpty()) {
            sendOrCancel(emitter, cancelled, event, group);
        }
    }

    private boolean sendStatus(
            SseEmitter emitter,
            AtomicBoolean cancelled,
            String stage,
            String message
    ) {
        Map<String, String> data = Map.of("stage", stage, "message", message);
        return send(emitter, cancelled, "status", data);
    }

    private void sendStatusOrCancel(
            SseEmitter emitter,
            AtomicBoolean cancelled,
            String stage,
            String message
    ) {
        if (!sendStatus(emitter, cancelled, stage, message)) {
            throw new StreamCancelledException();
        }
    }

    private void sendHeartbeat(SseEmitter emitter, AtomicBoolean cancelled) {
        if (cancelled.get()) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().comment("recipe-stream-heartbeat"));
        } catch (IOException | IllegalStateException exception) {
            cancelled.set(true);
        }
    }

    private void sendError(SseEmitter emitter, AtomicBoolean cancelled, String message) {
        if (!cancelled.get()) {
            send(emitter, cancelled, "error", Map.of("message", message));
        }
    }

    private boolean send(SseEmitter emitter, AtomicBoolean cancelled, String event, Object data) {
        if (cancelled.get()) {
            return false;
        }
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
            return true;
        } catch (IOException | IllegalStateException exception) {
            cancelled.set(true);
            return false;
        }
    }

    private void sendOrCancel(
            SseEmitter emitter,
            AtomicBoolean cancelled,
            String event,
            Object data
    ) {
        if (!send(emitter, cancelled, event, data)) {
            throw new StreamCancelledException();
        }
    }

    private void requireActive(AtomicBoolean cancelled) {
        if (cancelled.get() || Thread.currentThread().isInterrupted()) {
            throw new StreamCancelledException();
        }
    }

    private void validateRecipe(RecipeGenerateResponse response) {
        boolean validTitle = response != null && hasText(response.title()) && hasText(response.summary());
        boolean validIngredient = response != null && response.ingredients().stream()
                .anyMatch(item -> item != null && hasText(item.name()));
        boolean validStep = response != null && response.steps().stream()
                .anyMatch(item -> item != null && (hasText(item.title()) || hasText(item.description())));
        if (!validTitle || !validIngredient || !validStep) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 返回的菜谱内容不完整，请点击重试");
        }
    }

    private String errorMessage(Throwable exception) {
        Throwable current = exception;
        while (current.getCause() != null && !(current instanceof ResponseStatusException)) {
            current = current.getCause();
        }
        if (current instanceof ResponseStatusException statusException
                && statusException.getReason() != null
                && !statusException.getReason().isBlank()) {
            return statusException.getReason();
        }
        if (current instanceof SocketTimeoutException) {
            return "AI 生成超时，请检查网络后重试";
        }
        return "AI 菜谱生成暂时失败，请检查网络后重试";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static java.util.concurrent.ThreadFactory daemonThreadFactory(String prefix) {
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(prefix + "-" + thread.getId());
            thread.setDaemon(true);
            return thread;
        };
    }

    private static final class StreamCancelledException extends RuntimeException {
    }
}
