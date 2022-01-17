package me.itzg.melderbot.storage;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface MemberLinkRepository extends ReactiveMongoRepository<MemberLink, String> {

    Mono<MemberLink> findByCode(String code);
}
