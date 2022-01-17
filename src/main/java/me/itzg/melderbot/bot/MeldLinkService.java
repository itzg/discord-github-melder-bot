package me.itzg.melderbot.bot;

import discord4j.core.object.entity.User;
import java.time.Instant;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import me.itzg.melderbot.config.AppProperties;
import me.itzg.melderbot.integrations.RoleHandlingService;
import me.itzg.melderbot.integrations.github.model.GithubUser;
import me.itzg.melderbot.storage.Member;
import me.itzg.melderbot.storage.MemberLink;
import me.itzg.melderbot.storage.MemberLinkRepository;
import me.itzg.melderbot.storage.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class MeldLinkService {

    private final AppProperties appProperties;
    private final MemberRepository memberRepository;
    private final MemberLinkRepository memberLinkRepository;
    private final RoleHandlingService roleHandlingService;
    private final Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    public MeldLinkService(AppProperties appProperties,
        MemberRepository memberRepository,
        MemberLinkRepository memberLinkRepository,
        RoleHandlingService roleHandlingService
    ) {
        this.appProperties = appProperties;
        this.memberRepository = memberRepository;
        this.memberLinkRepository = memberLinkRepository;
        this.roleHandlingService = roleHandlingService;
    }

    public Mono<MeldSetupResult> handleMeldLink(User discordUser, String serverId) {
        return memberRepository.findByDiscordId(discordUser.getId().asString())
            .switchIfEmpty(Mono.defer(() ->
                memberRepository.save(
                    Member.builder()
                        .discordId(discordUser.getId().asString())
                        .discordUsername(discordUser.getUsername())
                        .discordDiscriminator(discordUser.getDiscriminator())
                        .serverIds(Set.of(serverId))
                        .build()
                )))
            .flatMap(member -> {
                final Mono<Member> updateMember;
                if (!member.getServerIds().contains(serverId)) {
                    final HashSet<String> serverIds = new HashSet<>(member.getServerIds());
                    serverIds.add(serverId);
                    updateMember = memberRepository.save(member.withServerIds(serverIds));
                }
                else {
                    updateMember = Mono.empty();
                }

                if (member.getGithubUsername() == null) {
                    log.debug("Generating meld url for {} on {}", discordUser, serverId);
                    return updateMember.then(
                            generateMemberLink(member)
                                .map(memberLink -> generateMeldUrlWithCode(memberLink.getCode()))
                                .map(MeldSetupResult::needsMeld)
                        );
                } else {
                    return updateMember
                        .flatMap(roleHandlingService::processInitialRoleAssignments)
                        .then(Mono.just(MeldSetupResult.noMeldNeeded()));
                }
            });
    }

    private String generateMeldUrlWithCode(String code) {
        return UriComponentsBuilder.fromUriString(appProperties.getBaseUrl())
            .pathSegment("meld", code)
            .build()
            .toUriString();
    }

    public Mono<MemberLink> generateMemberLink(Member member) {
        final byte[] codeBytes = new byte[appProperties.getMemberLinkCodeLength()];
        ThreadLocalRandom.current().nextBytes(codeBytes);
        final String code = encoder.encodeToString(codeBytes);

        return memberLinkRepository.save(
            new MemberLink()
                .setMemberId(member.getId())
                .setExpiration(Instant.now().plus(appProperties.getMemberLinkExpiration()))
                .setCode(code)
        );
    }

    public Mono<Member> processMeldCode(String code, GithubUser githubUser) {
        return memberLinkRepository.findByCode(code)
            .switchIfEmpty(Mono.error(new CodeNotFoundException("Invalid or expired code")))
            .flatMap(memberLink ->
                memberLinkRepository.delete(memberLink)
                    .then(Mono.just(memberLink))
            )
            .flatMap(memberLink -> memberRepository.findById(memberLink.getMemberId()))
            .switchIfEmpty(Mono.error(new IncompleteScenarioException("Unable to find Discord entry")))
            .flatMap(member -> memberRepository.save(
                member
                    .withGithubId(githubUser.id())
                    .withGithubUsername(githubUser.login())
            ));
    }
}
