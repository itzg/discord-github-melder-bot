package me.itzg.melderbot.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("app")
@Component
@Data
@Validated
public class AppProperties {
    @NotBlank
    String discordBotToken;

    @NotNull
    Long discordApplicationId;

    @NotBlank
    String baseUrl;

    @Min(1)
    int memberLinkCodeLength = 10;

    Duration memberLinkExpiration = Duration.ofHours(24);

    @NotEmpty
    Map<RoleType, @NotBlank String> roleName = Map.of(RoleType.CONTRIBUTOR, "Contributor");

    @NotEmpty
    List<@Pattern(regexp = "[A-Za-z0-9-]+/[A-Za-z0-9-]+", message = "org/repo") String> githubRepos;

    @Min(1)
    int minimumContributionCount = 1;

    @NotBlank
    String roleAddedReason = "You have been identified as a Github contributor";

    /**
     * If supplied, webhooks will be automatically configured for all of the {@link #githubRepos}
     */
    String githubAccessToken;

    /**
     * When set, webhook calls will be verified using the HMAC SHA256 described here
     * https://docs.github.com/en/developers/webhooks-and-events/webhooks/securing-your-webhooks
     */
    String githubWebhookSecret;

}
