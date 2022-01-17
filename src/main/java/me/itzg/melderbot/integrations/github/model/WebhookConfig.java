package me.itzg.melderbot.integrations.github.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
public record WebhookConfig(
    String url,
    @JsonProperty("content_type") WebhookContentType contentType,
    String secret
) {

}
