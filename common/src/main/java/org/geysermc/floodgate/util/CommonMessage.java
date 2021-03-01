package org.geysermc.floodgate.util;

import lombok.Getter;
import org.geysermc.floodgate.command.CommandMessage;

public enum CommonMessage implements CommandMessage {
    NOT_A_PLAYER("Please head over to your Minecraft Account and link from there."),
    CHECK_CONSOLE("Contatta un amministratore!"),
    IS_LINKED_ERROR("&cErrore durante il controllo di un giocatore. " + CHECK_CONSOLE);

    @Getter private final String message;

    CommonMessage(String message) {
        this.message = message.replace('&', COLOR_CHAR);
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
