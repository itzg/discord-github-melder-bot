package me.itzg.melderbot.integrations;

import reactor.core.publisher.Mono;

public class BadConfigurationException extends Exception {

    public BadConfigurationException(String msg) {
        super(msg);
    }
}
