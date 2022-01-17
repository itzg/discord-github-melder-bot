package me.itzg.melderbot.web;

import java.util.Map;
import me.itzg.melderbot.config.AppProperties;
import me.itzg.melderbot.config.RoleType;
import me.itzg.melderbot.integrations.RoleAssignmentResult;
import me.itzg.melderbot.integrations.RoleAssignmentResult.Status;
import me.itzg.melderbot.storage.Member;
import me.itzg.melderbot.storage.MemberLink;
import me.itzg.melderbot.storage.RoleAssignment;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Profile("dev")
@Controller
@RequestMapping("/dev")
public class DevController {

    private final ReactiveMongoTemplate mongoTemplate;
    private final AppProperties appProperties;

    public DevController(ReactiveMongoTemplate mongoTemplate, AppProperties appProperties) {
        this.mongoTemplate = mongoTemplate;
        this.appProperties = appProperties;
    }

    @DeleteMapping("/all-collections")
    @ResponseBody
    public Mono<Void> wipe() {
        return Flux.just(Member.class, MemberLink.class, RoleAssignment.class)
            .flatMap(cls -> mongoTemplate.remove(cls).all())
            .then();
    }

    @GetMapping("/views/meld/success")
    public Mono<Rendering> meldSuccess(@RequestParam RoleAssignmentResult.Status assignmentStatus) {
        return Mono.just(
            Rendering.view("meld/success")
                .modelAttribute("member", Member.builder()
                    .discordUsername("user")
                    .discordDiscriminator("1234")
                    .githubUsername("user")
                    .build()
                )
                .modelAttribute("rolesByStatus",
                    Map.of(assignmentStatus, appProperties.getRoleName().get(RoleType.CONTRIBUTOR)))
                .modelAttribute("repos", appProperties.getGithubRepos())
                .build()
        );
    }

}
