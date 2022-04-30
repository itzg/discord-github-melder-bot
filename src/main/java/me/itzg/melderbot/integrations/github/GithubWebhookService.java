package me.itzg.melderbot.integrations.github;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import me.itzg.melderbot.config.AppProperties;
import me.itzg.melderbot.integrations.github.model.GithubWebhook;
import me.itzg.melderbot.integrations.github.model.WebhookConfig;
import me.itzg.melderbot.integrations.github.model.WebhookContentType;
import me.itzg.melderbot.web.WebhookController;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class GithubWebhookService {

    private final Optional<WebClient> githubAppWebClient;
    private final AppProperties appProperties;

    public GithubWebhookService(@Qualifier("app") Optional<WebClient> githubAppWebClient,
        AppProperties appProperties
    ) {
        this.githubAppWebClient = githubAppWebClient;
        this.appProperties = appProperties;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void onStartup() {
        if (githubAppWebClient.isPresent()) {
            log.info("Setting up webhooks");
            Flux.fromIterable(appProperties.getGithubRepos())
                .flatMap(
                    repo -> setupWebHooks(repo, githubAppWebClient.get()))
                .onErrorResume(throwable -> {
                    log.warn("Failed to install webhook."
                        + " If a 401 is reported, then check your access token."
                        + " Message: {}", throwable.getMessage());
                    log.trace("Failed to install webhook, details: ", throwable);
                    return Mono.empty();
                })
                .blockLast();
        }
    }

    private Mono<?> setupWebHooks(String repo, WebClient webClient) {
        final String[] parts = GithubService.splitRepoParts(repo);

        final String webhookUrl = buildWebhookUrl();
        log.debug("Checking for webhook in repo={} to url={}", repo, webhookUrl);

        return webClient.get()
            .uri("/repos/{owner}/{repo}/hooks", parts[0], parts[1])
            .retrieve()
            .bodyToFlux(GithubWebhook.class)
            .checkpoint("Get webhooks from "+repo)
            .filter(githubWebhook -> githubWebhook.config().url().trim().equals(webhookUrl))
            .doOnNext(webhook -> log.debug("Existing webhook={}", webhook))
            .hasElements()
            .flatMap(exists -> exists ? Mono.empty() :
                webClient.post()
                .uri("/repos/{owner}/{repo}/hooks", parts[0], parts[1])
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(GithubWebhook.create(
                    webhookUrl,
                    appProperties.getGithubWebhookSecret(),
                    List.of("issues", "pull_request", "discussion")
                ))
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse ->
                    clientResponse.bodyToMono(String.class)
                        .doOnNext(body -> log.warn("Client error: {}", body))
                        .then(clientResponse.createException()))
                .toBodilessEntity()
                .checkpoint("Create webhook in "+repo)
                .doOnNext(entity -> log.debug("Webhook created at={}", entity.getHeaders().getLocation())))
            ;
    }

    private String buildWebhookUrl() {
        return UriComponentsBuilder.fromUriString(appProperties.getBaseUrl().trim())
            .path(WebhookController.GITHUB_WEBHOOK_PATH)
            .build()
            .toUriString();
    }
}
