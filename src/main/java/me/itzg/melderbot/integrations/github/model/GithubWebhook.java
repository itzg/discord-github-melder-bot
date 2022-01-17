package me.itzg.melderbot.integrations.github.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;

@JsonInclude(Include.NON_NULL)
public record GithubWebhook(
    Integer id,
    WebhookConfig config,
    List<String> events
) {
    public static GithubWebhook create(String url, String secret, List<String> events) {
        return new GithubWebhook(null,
            new WebhookConfig(url, WebhookContentType.json, secret),
            events);
    }
}
