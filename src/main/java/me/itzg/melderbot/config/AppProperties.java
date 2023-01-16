package me.itzg.melderbot.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import java.util.List;
import java.util.Map;
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
     * When set, webhook calls will be verified using the HMAC SHA256 described
     * <a href="https://docs.github.com/en/developers/webhooks-and-events/webhooks/securing-your-webhooks">here</a>
     */
    String githubWebhookSecret;

    /**
     * This is intended to only be changed for testing
     */
    @NotBlank
    String topLevelCommand = "meld";

}
