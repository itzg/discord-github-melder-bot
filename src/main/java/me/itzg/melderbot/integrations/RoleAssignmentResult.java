package me.itzg.melderbot.integrations;

import me.itzg.melderbot.config.RoleType;

public record RoleAssignmentResult(RoleType roleType, Status status) {
    public enum Status {
        ALREADY_ASSIGNED,
        ADDED_SOME_ASSIGNMENTS,
        NO_ROLES_ASSIGNED
    }
}
