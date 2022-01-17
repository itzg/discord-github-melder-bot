package me.itzg.melderbot.integrations.github.model;

import lombok.Data;

public record GithubUser(String login, int id) {
}
