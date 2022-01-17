package me.itzg.melderbot.storage;

import java.time.Instant;
import me.itzg.melderbot.config.RoleType;
import org.springframework.data.mongodb.core.index.CompoundIndex;

@CompoundIndex(unique = true, def = "{'memberId':1, 'roleType':1, 'discordServerId':1")
public record RoleAssignment(
    String id,
    Instant ts,
    String memberId,
    RoleType roleType,
    String discordServerId
) {
    public static RoleAssignment create(String memberId,
        RoleType roleType,
        String discordServerId) {
        return new RoleAssignment(null, Instant.now(), memberId, roleType, discordServerId);
    }
}
