package me.itzg.melderbot.bot;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CodeNotFoundException extends Exception {

    public CodeNotFoundException(String message) {
        super(message);
    }
}
