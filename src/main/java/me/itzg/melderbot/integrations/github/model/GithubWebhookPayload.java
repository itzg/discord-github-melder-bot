package me.itzg.melderbot.integrations.github.model;

public record GithubWebhookPayload(
    String action,
    GithubUser sender,
    GithubRepository repository
) {

}
