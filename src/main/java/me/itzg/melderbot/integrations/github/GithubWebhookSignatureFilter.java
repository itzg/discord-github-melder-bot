package me.itzg.melderbot.integrations.github;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import me.itzg.melderbot.config.AppProperties;
import me.itzg.melderbot.integrations.github.GithubWebhookService;
import me.itzg.melderbot.web.InvalidSignatureException;
import me.itzg.melderbot.web.WebhookController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Intercepts the request body and performs the HMAC SHA256 verification described in https://docs.github.com/en/developers/webhooks-and-events/webhooks/securing-your-webhooks#validating-payloads-from-github
 */
@ConditionalOnProperty(prefix = "app", name = "github-webhook-secret")
@Component
@Slf4j
public class GithubWebhookSignatureFilter implements WebFilter {

    protected static final String ALGORITHM = "HmacSHA256";
    private final GithubWebhookService githubWebhookService;
    private final AppProperties appProperties;

    private static final ServerWebExchangeMatcher matcher = ServerWebExchangeMatchers.pathMatchers(
        HttpMethod.POST, WebhookController.GITHUB_WEBHOOK_PATH);

    public GithubWebhookSignatureFilter(GithubWebhookService githubWebhookService, AppProperties appProperties) {
        this.githubWebhookService = githubWebhookService;
        this.appProperties = appProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return matcher.matches(exchange)
            .flatMap(matchResult ->
                matchResult.isMatch() ?
                    verify(exchange, chain)
                    : chain.filter(exchange)
            );
    }

    private Mono<Void> verify(ServerWebExchange exchange, WebFilterChain chain) {
        final ServerHttpRequest req = exchange.getRequest();
        final String deliveryId = req.getHeaders().getFirst("X-GitHub-Delivery");

        log.debug("Verifying webhook deliveryId={}", deliveryId);

        final String givenSignature = req.getHeaders().getFirst("X-Hub-Signature-256");
        if (givenSignature == null) {
            log.debug("Missing signature header");
            exchange.getResponse()
                .setStatusCode(HttpStatus.FORBIDDEN);
            return Mono.empty();
        }

        final SecretKeySpec secretKey = new SecretKeySpec(
            appProperties.getGithubWebhookSecret().getBytes(StandardCharsets.UTF_8), ALGORITHM
        );
        final Mac hmac;
        try {
            hmac = Mac.getInstance(ALGORITHM);
            hmac.init(secretKey);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to create HMAC instance", e);
            exchange.getResponse()
                .setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return Mono.empty();
        }

        @SuppressWarnings("ReactiveStreamsUnusedPublisher") // used below in decorator
        final Flux<DataBuffer> verifiedBody = req.getBody()
            .doOnNext(dataBuffer -> hmac.update(dataBuffer.asByteBuffer()))
            .doOnComplete(() -> {
                final String computedSignature = HexFormat.of()
                    .formatHex(hmac.doFinal());

                if (!givenSignature.equals("sha256=" + computedSignature)) {
                    log.warn("Mismatch signature for webhook deliveryId={} from={} agent={}",
                        deliveryId, req.getRemoteAddress(),
                        req.getHeaders().getFirst("user-agent")
                    );
                    throw new InvalidSignatureException("Invalid payload signature");
                }
            });

        return chain.filter(
            exchange.mutate()
                .request(new ServerHttpRequestDecorator(exchange.getRequest()) {
                    @SuppressWarnings("NullableProblems") // IntelliJ is confused
                    @Override
                    public Flux<DataBuffer> getBody() {
                        return verifiedBody;
                    }
                })
                .build()
        );

    }
}
