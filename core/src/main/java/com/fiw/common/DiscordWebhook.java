package com.fiw.common;

import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class DiscordWebhook {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private DiscordWebhook() {
    }

    /** Fire-and-forget; never blocks the caller (safe on the server thread). */
    public static void send(String webhookUrl, String content, FiwPlatform platform) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("content", content.length() > 1900 ? content.substring(0, 1900) + "…" : content);

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                    .build();
        } catch (IllegalArgumentException exception) {
            platform.warn("Invalid Discord webhook URL: " + exception.getMessage());
            return;
        }

        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        platform.warn("Discord webhook failed: " + throwable.getMessage());
                    } else if (response.statusCode() >= 300) {
                        platform.warn("Discord webhook returned HTTP " + response.statusCode());
                    }
                });
    }
}