package me.itzg.melderbot.integrations.github;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

import lombok.extern.slf4j.Slf4j;
import me.itzg.melderbot.integrations.github.model.GithubRepoContributor;
import me.itzg.melderbot.integrations.github.model.GithubUser;
import me.itzg.melderbot.storage.Member;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class GithubService {


    private final WebClient githubUserWebClient;
    private final WebClient githubPublicWebClient;

    public GithubService(
        @Qualifier("user") WebClient githubUserWebClient,
        @Qualifier("public") WebClient githubPublicWebClient
        ) {
        this.githubUserWebClient = githubUserWebClient;
        this.githubPublicWebClient = githubPublicWebClient;
    }

    public static String[] splitRepoParts(String repo) {
        return repo.split("/", 2);
    }

    public Mono<GithubUser> getCurrentUser(OAuth2AuthorizedClient authorizedClient) {
        return githubUserWebClient.get()
            .uri("/user")
            .attributes(oauth2AuthorizedClient(authorizedClient))
            .retrieve()
            .bodyToMono(GithubUser.class);
    }

    public Mono<Integer> contributionCount(Member member, String repo) {
        if (member.getGithubId() <= 0) {
            return Mono.error(() -> new IllegalArgumentException("Member is missing Github ID"));
        }

        final String[] parts = GithubService.splitRepoParts(repo);
        return githubPublicWebClient.get()
            .uri("/repos/{owner}/{repo}/contributors", parts[0], parts[1])
            .retrieve()
            .bodyToFlux(GithubRepoContributor.class)
            .filter(githubRepoContributor -> githubRepoContributor.id() == member.getGithubId())
            .next()
            .map(GithubRepoContributor::contributions)
            .doOnNext(count -> log.debug("{} has {} contributions in {}", member, count, repo));
    }

}
