package me.itzg.melderbot.integrations.github;

import java.util.List;
import me.itzg.melderbot.config.AppProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;

@Configuration
public class GithubClients {

    private final AppProperties appProperties;

    public GithubClients(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean @Qualifier("user")
    public WebClient githubUserWebClient(Builder webClientBuilder,
        ReactiveClientRegistrationRepository clientRegistrationRepo,
        ServerOAuth2AuthorizedClientRepository authorizedClientRepo
    ) {
        return webClientBuilder
            .baseUrl("https://api.github.com")
            .filter(new ServerOAuth2AuthorizedClientExchangeFilterFunction(
                clientRegistrationRepo,
                authorizedClientRepo)
            )
            .build();
    }

    @Bean @Qualifier("public")
    public WebClient githubPublicWebClient(Builder webClientBuilder) {
        return webClientBuilder
            .baseUrl("https://api.github.com")
            .defaultHeaders(httpHeaders ->
                httpHeaders.setAccept(List.of(MediaType.parseMediaType("application/vnd.github.v3+json")))
            )
            .build();
    }

    @ConditionalOnProperty(prefix = "app", name = "github-access-token")
    @Bean @Qualifier("app")
    public WebClient githubAppWebClient(Builder webClientBuilder) {
        return webClientBuilder
            .baseUrl("https://api.github.com")
            .defaultHeaders(httpHeaders ->
                httpHeaders.setAccept(List.of(MediaType.parseMediaType("application/vnd.github.v3+json")))
            )
            .defaultHeaders(httpHeaders ->
                    httpHeaders.setBasicAuth("", appProperties.getGithubAccessToken())
                )
            .build();
    }
}
