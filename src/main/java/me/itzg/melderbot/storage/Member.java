package me.itzg.melderbot.storage;

import java.util.Set;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Getter @ToString @EqualsAndHashCode
@Builder
@With
public class Member {
    @Indexed
    String id;

    String discordId;
    String discordUsername;
    String discordDiscriminator;

    String githubUsername;
    @Indexed
    int githubId;

    Set<String> serverIds;
}
