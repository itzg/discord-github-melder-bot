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
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import me.itzg.melderbot.config.AppProperties;
import org.reactivestreams.Publisher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class BotRunner implements ApplicationRunner, Closeable {

    protected static final String LINK_WITH_GITHUB = "link-with-github";
    private final AppProperties appProperties;
    private final DiscordClient discordClient;
    private final MeldLinkService meldLinkService;
    private final String topLevelCommand;
    private Disposable botSubscription;

    public BotRunner(AppProperties appProperties,
        DiscordClient discordClient,
        MeldLinkService meldLinkService
    ) {
        this.appProperties = appProperties;
        this.discordClient = discordClient;
        this.meldLinkService = meldLinkService;
        this.topLevelCommand = appProperties.getTopLevelCommand();
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        botSubscription =
            registerCommands()
                .then(
                    discordClient.login()
                        .flatMapMany(gateway -> gateway.on(ApplicationCommandInteractionEvent.class))
                        .switchMap(appCommandEvent -> {
                            if (appCommandEvent.getCommandName().equals(topLevelCommand)) {
                                return handleMeld(appCommandEvent);
                            } else {
                                return Mono.error(new IllegalArgumentException("Unknown command"));
                            }
                        })
                        .onErrorContinue((throwable, appCommandEvent) -> {
                            log.warn("Failed to process command interaction event={}", appCommandEvent, throwable);
                        })
                        .then()
                )
                .subscribe();
    }

    private Mono<?> registerCommands() {
        final ApplicationService applicationService = discordClient.getApplicationService();
        return Mono.when(
            applicationService
                .createGlobalApplicationCommand(
                    appProperties.getDiscordApplicationId(),
                    ApplicationCommandRequest.builder()
                        .name(topLevelCommand)
                        // https://discord.com/developers/docs/interactions/application-commands#application-command-object-application-command-types
                        .type(1)
                        .description("Melds Discord users with Github users")
                        .addOption(ApplicationCommandOptionData.builder()
                            .name(LINK_WITH_GITHUB)
                            .type(1)
                            .description("Links your Discord user to a Github user")
                            .build())
                        .build()
                )
                .doOnNext(applicationCommandData -> log.debug("Registered command {}: {}", topLevelCommand, applicationCommandData))
            ,
            applicationService.getGlobalApplicationCommands(appProperties.getDiscordApplicationId())
                .flatMap(applicationCommandData -> {
                    if (!applicationCommandData.name().equals(topLevelCommand)) {
                        return applicationService.deleteGlobalApplicationCommand(
                            appProperties.getDiscordApplicationId(),
                            Long.parseLong(applicationCommandData.id())
                        )
                            .doOnNext(unused -> log.debug("Deleted old command: {}", applicationCommandData));
                    } else {
                        return Mono.empty();
                    }
                })
                .then()
        );
    }

    private Publisher<?> handleMeld(ApplicationCommandInteractionEvent appCommandEvent) {
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
        if (subcommandOption.name().equals(LINK_WITH_GITHUB)) {
            return linkWithGithub(appCommandEvent);
        } else {
            return Mono.error(() -> new IllegalArgumentException(String.format("Unknown subcommand: %s", data.name().get())));
        }
    }

    private Mono<Void> linkWithGithub(ApplicationCommandInteractionEvent appCommandEvent) {
        return Mono.justOrEmpty(appCommandEvent.getInteraction().getGuildId())
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
    public void close() throws IOException {
        if (botSubscription != null) {
            botSubscription.dispose();
        }
    }
}
