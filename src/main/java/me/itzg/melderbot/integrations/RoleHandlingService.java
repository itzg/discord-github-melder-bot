package me.itzg.melderbot.integrations;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.rest.entity.RestGuild;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import me.itzg.melderbot.config.AppProperties;
import me.itzg.melderbot.config.RoleType;
import me.itzg.melderbot.integrations.RoleAssignmentResult.Status;
import me.itzg.melderbot.integrations.github.GithubService;
import me.itzg.melderbot.storage.Member;
import me.itzg.melderbot.storage.MemberRepository;
import me.itzg.melderbot.storage.RoleAssignment;
import me.itzg.melderbot.storage.RoleAssignmentRepository;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class RoleHandlingService {

    private final AppProperties appProperties;
    private final GithubService githubService;
    private final DiscordClient discordClient;
    private final MemberRepository memberRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private Disposable checkPendingSubscription;

    public RoleHandlingService(AppProperties appProperties,
        GithubService githubService,
        DiscordClient discordClient,
        MemberRepository memberRepository,
        RoleAssignmentRepository roleAssignmentRepository
    ) {
        this.appProperties = appProperties;
        this.githubService = githubService;
        this.discordClient = discordClient;
        this.memberRepository = memberRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
    }

    public Mono<RoleAssignmentResult> processInitialRoleAssignments(Member member) {
        log.debug("Processing initial role assignments for member={}", member);
        return Flux.fromIterable(appProperties.getGithubRepos())
            .flatMap(repo -> githubService.contributionCount(member, repo))
            .onErrorContinue((throwable1, repo) ->
                log.warn("Failed to retrieve contribution counts from {}", repo, throwable1))
            .any(this::hasEnoughContributions)
            .flatMap(hasAny -> hasAny ?
                assignRole(RoleType.CONTRIBUTOR, member)
                : Mono.just(new RoleAssignmentResult(RoleType.CONTRIBUTOR, Status.NO_ROLES_ASSIGNED))
            )
            .doOnError(throwable -> log.warn("Failure during processInitialRoleAssignments", throwable));
    }

    /**
     * Schedules a check of the given user's contributions for the repo.
     */
    public Mono<?> processRepoContributionCreated(String repoName, int githubUserId) {
        if (!appProperties.getGithubRepos().contains(repoName)) {
            log.warn("Skipped processing of {} since it is not configured", repoName);
            return Mono.empty();
        }

        log.debug("Checking for contributions in repo={} from githubUserId={}", repoName, githubUserId);
        return memberRepository.findByGithubId(githubUserId)
            .filterWhen(member ->
                githubService.contributionCount(member, repoName)
                    .map(this::hasEnoughContributions)
            )
            .flatMap(member -> assignRole(RoleType.CONTRIBUTOR, member))
            .doOnNext(result -> log.debug("Role assignment result={} for githubUserId={}",
                result, githubUserId));
    }

    @PreDestroy
    public void stopPendingContributionCheck() {
        if (checkPendingSubscription != null) {
            checkPendingSubscription.dispose();
            checkPendingSubscription = null;
        }
    }

    private Mono<RoleAssignmentResult> assignRole(RoleType roleType, Member member) {
        return Flux.fromIterable(member.getServerIds())
            .filterWhen(serverId ->
                roleAssignmentRepository.existsByMemberIdAndRoleTypeAndDiscordServerId(
                    member.getId(),
                    roleType,
                    serverId
                )
                    .map(exists -> !exists)
            )
            .flatMap(serverId -> assignRoleOnServer(roleType, serverId, member))
            .reduce(Integer::sum)
            .map(countAssigned -> countAssigned > 0 ?
                new RoleAssignmentResult(roleType, Status.ADDED_SOME_ASSIGNMENTS)
                : new RoleAssignmentResult(roleType, Status.ALREADY_ASSIGNED)
            )
            .doOnNext(roleAssignmentResult ->
                log.debug("Assigned result={} for member={}", roleAssignmentResult, member)
            );
    }

    private Mono<Integer> assignRoleOnServer(RoleType roleType, String serverId, Member member) {
        final RestGuild server = discordClient.getGuildById(Snowflake.of(serverId));
        final Snowflake userId = Snowflake.of(member.getDiscordId());

        return server
            .getRoles()
            .filter(roleData -> roleData.name().equals(appProperties.getRoleName().get(roleType)))
            .next()
            .switchIfEmpty(Mono.error(() -> {
                log.error("The serverId={} is missing the role={}. Unable to assign to member={}",
                    serverId, appProperties.getRoleName(), member);
                return new BadConfigurationException("The server does not have the expected role");
            }))
            .filterWhen(roleData ->
                server.getMember(userId)
                    // doesn't have role yet?
                    .filter(memberData -> !memberData.roles().contains(roleData.id()))
                    .doOnNext(memberData -> log.debug("Member={} already had role={} on server={}", member, roleData, serverId))
                    .map(memberData -> true)
            )
            .doOnNext(roleData -> log.debug("Adding role={} to member={} on server={}", roleData, member, serverId))
            .flatMap(roleData -> Mono.when(
                    server.addMemberRole(
                        userId,
                        Snowflake.of(roleData.id()),
                        appProperties.getRoleAddedReason()
                    ),
                    roleAssignmentRepository.save(
                        RoleAssignment.create(member.getId(), roleType, serverId)
                    )
                )
                    .then(Mono.just(1))
            )
            .switchIfEmpty(Mono.just(0));
    }

    private boolean hasEnoughContributions(Integer contributionsPerRepo) {
        return contributionsPerRepo >= appProperties.getMinimumContributionCount();
    }

    public Flux<String> getRolesAssigned(Member member) {
        return roleAssignmentRepository.findByMemberId(member.getId())
            .doOnNext(roleAssignment -> log.debug("Found roleAssignment={} for member={}", roleAssignment, member))
            .map(roleAssignment -> appProperties.getRoleName().get(roleAssignment.roleType()));
    }
}
