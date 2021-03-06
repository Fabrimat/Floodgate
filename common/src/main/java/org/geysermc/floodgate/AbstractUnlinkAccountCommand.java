package org.geysermc.floodgate;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.geysermc.floodgate.command.CommandMessage;
import org.geysermc.floodgate.util.CommonMessage;
import org.geysermc.floodgate.util.ICommandUtil;

import java.util.UUID;

@AllArgsConstructor
public class AbstractUnlinkAccountCommand<P, C extends ICommandUtil<P>> {
    private final PlayerLink link;
    @Getter(AccessLevel.PROTECTED)
    private final C commandUtil;

    public void execute(P player, UUID uuid) {
        if (!PlayerLink.isEnabledAndAllowed()) {
            sendMessage(player, Message.LINKING_NOT_ENABLED);
            return;
        }
        link.isLinkedPlayer(uuid).whenComplete((linked, throwable) -> {
            if (throwable != null) {
                sendMessage(player, CommonMessage.IS_LINKED_ERROR);
                return;
            }
            if (!linked) {
                sendMessage(player, Message.NOT_LINKED);
                return;
            }
            link.unlinkPlayer(uuid).whenComplete((aVoid, throwable1) ->
                    sendMessage(player, throwable1 == null ? Message.UNLINK_SUCCESS : Message.UNLINK_ERROR)
            );
        });
    }

    private void sendMessage(P player, CommandMessage message, Object... args) {
        commandUtil.sendMessage(player, message, args);
    }

    public enum Message implements CommandMessage {
        NOT_LINKED("&cIl tuo account non è collegato"),
        UNLINK_SUCCESS("&cCollegamento rimosso con successo! Rientra per tornare al tuo account Bedrock"),
        UNLINK_ERROR("&cC'è stato un errore durante la rimozione del collegamento. " + CommonMessage.CHECK_CONSOLE),
        LINKING_NOT_ENABLED("&cCollegamento degli account non attivato su questo server.");

        @Getter
        private final String message;

        Message(String message) {
            this.message = message.replace('&', COLOR_CHAR);
        }
    }
}
