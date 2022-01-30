package me.itzg.melderbot.bot;

import java.util.Set;

public record MeldStatus(boolean githubLinked, Set<String> rolesAssigned) {

}
