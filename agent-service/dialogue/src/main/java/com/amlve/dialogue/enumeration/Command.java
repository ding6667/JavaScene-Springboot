package com.amlve.dialogue.enumeration;

import lombok.Getter;

@Getter
public enum Command {
    ANSWER("answer"),
    RECONFIRM("reconfirm");

    private final String command;

    Command(String command) {
        this.command = command;
    }

    public static Command fromString(String command) {
        for (Command c : values()) {
            if (c.command.equals(command)) {
                return c;
            }
        }
        throw new IllegalArgumentException("No command found for " + command);
    }
}

