package me.itzg.melderbot.web;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import me.itzg.melderbot.bot.MeldLinkService;
import me.itzg.melderbot.config.AppProperties;
import me.itzg.melderbot.integrations.RoleAssignmentResult;
import me.itzg.melderbot.integrations.github.GithubService;
import me.itzg.melderbot.integrations.RoleHandlingService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/" + WebPaths.MELD)
@Slf4j
public class MeldController {

    private final MeldLinkService meldLinkService;
    private final RoleHandlingService roleHandlingService;
    private final GithubService githubService;
    private final AppProperties appProperties;

    public MeldController(
        MeldLinkService meldLinkService,
        RoleHandlingService roleHandlingService,
        GithubService githubService,
        AppProperties appProperties
    ) {
        this.meldLinkService = meldLinkService;
        this.roleHandlingService = roleHandlingService;
        this.githubService = githubService;
        this.appProperties = appProperties;
    }

    @GetMapping("{code}")
    public Mono<Rendering> withCode(@PathVariable String code,
        @RegisteredOAuth2AuthorizedClient("github") OAuth2AuthorizedClient authorizedClient
    ) {
        return githubService.getCurrentUser(authorizedClient)
            .flatMap(githubUser -> meldLinkService.processMeldCode(code, githubUser))
            .flatMap(member -> Mono.zip(
                Mono.just(member),
                roleHandlingService.processInitialRoleAssignments(member)
            ))
            .map(results -> Rendering.view("meld/success")
                .modelAttribute("member", results.getT1())
                .modelAttribute("rolesByStatus", setupForModel(results.getT2()))
                .modelAttribute("repos", appProperties.getGithubRepos())
                .build()
            );
    }

    private Map<RoleAssignmentResult.Status, String> setupForModel(RoleAssignmentResult results) {
        return Map.of(
            results.status(), appProperties.getRoleName().get(results.roleType())
        );
    }
}
