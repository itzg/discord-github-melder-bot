package me.itzg.melderbot.storage;

import me.itzg.melderbot.config.RoleType;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface MemberRepository extends ReactiveMongoRepository<Member, String> {

    Mono<Member> findByDiscordId(String id);

    Mono<Member> findByGithubId(int githubId);
}
