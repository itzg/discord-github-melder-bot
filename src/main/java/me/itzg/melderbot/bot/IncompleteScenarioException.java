package me.itzg.melderbot.bot;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class IncompleteScenarioException extends Exception {

    public IncompleteScenarioException(String message) {
        super(message);
    }
}
