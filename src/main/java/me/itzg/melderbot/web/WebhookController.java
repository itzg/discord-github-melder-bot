package me.itzg.melderbot.web;

import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import me.itzg.melderbot.integrations.github.model.GithubWebhookPayload;
import me.itzg.melderbot.integrations.RoleHandlingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/webhook")
@Slf4j
public class WebhookController {

    public static final String GITHUB_WEBHOOK_PATH = "/webhook/github";
    private static final Map<String, Set<String>> acceptedActionsByEvent =
        Map.of(
            "issues", Set.of("opened"),
            "discussion", Set.of("created"),
            "pull_request", Set.of("opened")
            );

    private final RoleHandlingService roleHandlingService;

    public WebhookController(RoleHandlingService roleHandlingService) {
        this.roleHandlingService = roleHandlingService;
    }

    @PostMapping(value = "/github", headers = {"X-GitHub-Event=ping"})
    public Mono<Void> handleGithubWebhookPing(@RequestBody GithubWebhookPayload payload) {
        log.debug("Received webhook ping={}", payload);
        return Mono.empty();
    }

    @PostMapping(value = "/github")
    public Mono<Void> handleGithubWebhook(
        @RequestHeader("X-GitHub-Event") String event,
        @RequestBody GithubWebhookPayload payload
    ) {
        final Set<String> actions = acceptedActionsByEvent.get(event);
        if (actions != null && actions.contains(payload.action())) {
            log.debug("Processing webhook for event={} action={}", event, payload.action());

            return roleHandlingService.processRepoContributionCreated(
                    payload.repository().fullName(),
                    payload.sender().id()
                )
                .then();
        }
        else {
            log.debug("Ignoring event={} action={}", event, payload.action());
            return Mono.empty();
        }
    }
}
