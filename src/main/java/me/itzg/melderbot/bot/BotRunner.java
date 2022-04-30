package me.itzg.melderbot.bot;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.discordjson.json.ApplicationCommandInteractionData;
import discord4j.discordjson.json.ApplicationCommandInteractionOptionData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.service.ApplicationService;
import java.io.Closeable;
import lombok.extern.slf4j.Slf4j;
import me.itzg.melderbot.config.AppProperties;
import org.reactivestreams.Publisher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class BotRunner implements ApplicationRunner, Closeable {

    protected static final String CMD_LINK_WITH_GITHUB = "link-with-github";
    protected static final String CMD_STATUS = "status";

    private final AppProperties appProperties;
    private final DiscordClient discordClient;
    private final MeldLinkService meldLinkService;
    private final String topLevelCommand;
    private final ConfigurableApplicationContext applicationContext;
    private Disposable botSubscription;

    public BotRunner(AppProperties appProperties,
        DiscordClient discordClient,
        MeldLinkService meldLinkService,
        ConfigurableApplicationContext applicationContext
    ) {
        this.appProperties = appProperties;
        this.discordClient = discordClient;
        this.meldLinkService = meldLinkService;
        this.topLevelCommand = appProperties.getTopLevelCommand();
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        botSubscription =
            registerCommands()
                .then(
                    discordClient.login()
                        .checkpoint("login")
                        .flatMapMany(gateway -> gateway.on(ApplicationCommandInteractionEvent.class))
                        .checkpoint("application command interaction event")
                        .switchMap(appCommandEvent -> {
                            if (appCommandEvent.getCommandName().equals(topLevelCommand)) {
                                return handleMeldCommands(appCommandEvent);
                            } else {
                                return Mono.error(new IllegalArgumentException("Unknown command"));
                            }
                        })
                        .onErrorContinue((throwable, appCommandEvent) ->
                            log.warn("Failed to process command interaction event={}", appCommandEvent, throwable)
                        )
                        .then()
                )
                .subscribe(null, throwable -> {
                    log.error("BotRunner failed", throwable);
                    applicationContext.close();
                });
    }

    private Mono<?> registerCommands() {
        final ApplicationService applicationService = discordClient.getApplicationService();
        return applicationService
            .createGlobalApplicationCommand(
                appProperties.getDiscordApplicationId(),
                ApplicationCommandRequest.builder()
                    .name(topLevelCommand)
                    // https://discord.com/developers/docs/interactions/application-commands#application-command-object-application-command-types
                    .type(1)
                    .description("Melds Discord users with Github users")
                    .addOption(ApplicationCommandOptionData.builder()
                        .name(CMD_LINK_WITH_GITHUB)
                        .type(1)
                        .description("Links your Discord user to a Github user")
                        .build())
                    .addOption(ApplicationCommandOptionData.builder()
                        .name(CMD_STATUS)
                        .type(1)
                        .description("Check your meld status")
                        .build())
                    .build()
            )
            .doOnNext(applicationCommandData -> log.debug("Registered command {}: {}", topLevelCommand, applicationCommandData))
            .checkpoint("register commands");
    }

    private Publisher<?> handleMeldCommands(ApplicationCommandInteractionEvent appCommandEvent) {
        log.debug("Handling {} command from {}", topLevelCommand, appCommandEvent.getInteraction().getUser().getUsername());

        final Possible<ApplicationCommandInteractionData> possibleData = appCommandEvent.getInteraction()
            .getData().data();

        if (possibleData.isAbsent()) {
            return Mono.error(() -> new IllegalArgumentException("Missing subcommand data"));
        }

        final ApplicationCommandInteractionData data = possibleData.get();
        if (data.type().isAbsent() || data.type().get() != 1) {
            return Mono.error(() -> new IllegalArgumentException("Missing command data"));
        }

        if (data.options().isAbsent()) {
            return Mono.error(() -> new IllegalArgumentException("Missing subcommand"));
        }

        final ApplicationCommandInteractionOptionData subcommandOption = data.options().get().get(0);
        final String subcommandName = subcommandOption.name();
        if (subcommandName.equals(CMD_LINK_WITH_GITHUB)) {
            return linkWithGithub(appCommandEvent);
        } else if (subcommandName.equals(CMD_STATUS)) {
            return checkStatus(appCommandEvent);
        } else {
            return Mono.error(() -> new IllegalArgumentException(String.format("Unknown subcommand: %s", data.name().get())));
        }
    }

    private Mono<Void> checkStatus(ApplicationCommandInteractionEvent appCommandEvent) {
        return meldLinkService.getMeldStatus(appCommandEvent.getInteraction().getUser())
            .flatMap(meldStatus ->
                appCommandEvent.reply()
                    .withEphemeral(true)
                    .withContent(contentForMeldStatus(meldStatus))
            )
            .log();
    }

    private String contentForMeldStatus(MeldStatus meldStatus) {
        if (meldStatus == null) {
            return "Unable to determine your status";
        }

        if (!meldStatus.githubLinked()) {
            return String.format("You are not yet linked with your Github account. Use the %s subcommand to do that.",
                CMD_LINK_WITH_GITHUB);
        }

        if (meldStatus.rolesAssigned().isEmpty()) {
            return "Your Github account is linked, but no roles have been assigned.";
        } else {
            return String.format(
                "Your Github account is linked and you have been assigned the role%s %s.",
                meldStatus.rolesAssigned().size() > 1 ? "s" : "",
                String.join(", ", meldStatus.rolesAssigned())
            );
        }
    }

    private Mono<Void> linkWithGithub(ApplicationCommandInteractionEvent appCommandEvent) {
        return Mono.justOrEmpty(appCommandEvent.getInteraction().getGuildId())
            .checkpoint("lookup server ID")
            .flatMap(serverId ->
                meldLinkService.handleMeldLink(
                    appCommandEvent.getInteraction().getUser(),
                    serverId.asString()
                )
            )
            .flatMap(meldSetupResult -> {
                if (meldSetupResult.needingMeld()) {
                    return appCommandEvent.reply()
                        .withEphemeral(true)
                        .withContent("Click here to complete melding Discord with Github")
                        .withComponents(ActionRow.of(
                            Button.link(meldSetupResult.meldUrl(), "Meld")
                        ));
                } else {
                    return appCommandEvent.reply()
                        .withEphemeral(true)
                        .withContent("You have already melded Discord to your Github account");
                }
            })
            .onErrorResume(throwable -> {
                log.warn("Error occurred while processing {}", appCommandEvent, throwable);
                return appCommandEvent.reply()
                    .withEphemeral(true)
                    .withContent(String.format("ERROR: %s", throwable.getMessage()));
            });
    }

    @Override
    public void close() {
        if (botSubscription != null) {
            botSubscription.dispose();
        }
    }
}
