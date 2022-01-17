package me.itzg.melderbot.storage;

import java.time.Instant;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
public class MemberLink {
    String id;

    @Indexed
    String code;

    @Indexed(expireAfterSeconds = 0)
    Instant expiration;

    @Indexed
    String memberId;
}
