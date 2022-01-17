package me.itzg.melderbot.storage;

import me.itzg.melderbot.config.RoleType;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface RoleAssignmentRepository extends ReactiveMongoRepository<RoleAssignment, String> {

    Mono<Boolean> existsByMemberIdAndRoleTypeAndDiscordServerId(String memberId, RoleType roleType, String discordServerId);

}
