package me.itzg.melderbot.integrations.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubRepository(
    @JsonProperty("full_name") String fullName
) {

}
