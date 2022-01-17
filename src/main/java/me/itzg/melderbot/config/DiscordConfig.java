package me.itzg.melderbot.config;

import discord4j.core.DiscordClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiscordConfig {

    private final AppProperties appProperties;

    public DiscordConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public DiscordClient discordClient() {
        return DiscordClient.create(appProperties.getDiscordBotToken());
    }
}
