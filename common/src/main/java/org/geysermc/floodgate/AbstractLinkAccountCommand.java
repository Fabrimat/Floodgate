package org.geysermc.floodgate;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.geysermc.floodgate.command.CommandMessage;
import org.geysermc.floodgate.util.CommonMessage;
import org.geysermc.floodgate.util.ICommandUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@AllArgsConstructor
public class AbstractLinkAccountCommand<P, C extends ICommandUtil<P>> {
    private final Map<String, LinkRequest> activeLinkRequests = new HashMap<>();
    private final PlayerLink link;
    @Getter(AccessLevel.PROTECTED)
    private final C commandUtil;

    public void execute(P player, UUID uuid, String username, String[] args) {
        if (!PlayerLink.isEnabledAndAllowed()) {
            sendMessage(player, Message.LINK_REQUEST_DISABLED);
            return;
        }
        link.isLinkedPlayer(uuid).whenComplete((linked, throwable) -> {
            if (throwable != null) {
                sendMessage(player, CommonMessage.IS_LINKED_ERROR);
                return;
            }
            if (linked) {
                sendMessage(player, Message.ALREADY_LINKED);
                return;
            }
            // when the player is a Java player
            if (!AbstractFloodgateAPI.isBedrockPlayer(uuid)) {
                if (args.length != 1) {
                    sendMessage(player, Message.JAVA_USAGE);
                    return;
                }
                String code = String.format("%04d", new Random().nextInt(10000));
                String bedrockUsername = args[0];
                activeLinkRequests.put(username, new LinkRequest(username, uuid, code, bedrockUsername));
                sendMessage(player, Message.LINK_REQUEST_CREATED, bedrockUsername, username, code);
                return;
            }
            // when the player is a Bedrock player
            if (args.length != 2) {
                sendMessage(player, Message.BEDROCK_USAGE);
                return;
            }
            String javaUsername = args[0];
            String code = args[1];
            LinkRequest request = activeLinkRequests.getOrDefault(javaUsername, null);
            if (request != null && request.checkGamerTag(AbstractFloodgateAPI.getPlayer(uuid))) {
                if (request.getLinkCode().equals(code)) {
                    activeLinkRequests.remove(javaUsername); // Delete the request, whether it has expired or is successful
                    if (request.isExpired()) {
                        sendMessage(player, Message.LINK_REQUEST_EXPIRED);
                        return;
                    }
                    link.linkPlayer(uuid, request.getJavaUniqueId(), request.getJavaUsername()).whenComplete((aVoid, throwable1) -> {
                        if (throwable1 != null) {
                            sendMessage(player, Message.LINK_REQUEST_ERROR);
                            return;
                        }
                        commandUtil.kickPlayer(player, Message.LINK_REQUEST_COMPLETED, request.getJavaUsername());
                    });
                    return;
                }
                sendMessage(player, Message.INVALID_CODE);
                return;
            }
            sendMessage(player, Message.NO_LINK_REQUESTED);
        });
    }

    private void sendMessage(P player, CommandMessage message, Object... args) {
        commandUtil.sendMessage(player, message, args);
    }

    public enum Message implements CommandMessage {
        ALREADY_LINKED(
                "&cIl tuo account è già stato collegato!\n" +
                        "&cSe vuoi collegarlo ad un altro account digita &6/unlinkaccount&c e riprova."
        ),
        JAVA_USAGE("&cUtilizzo: /linkaccount <gamertag>"),
        LINK_REQUEST_CREATED(
                "&aEntra con %s su Bedrock e digita &6/linkaccount %s %s\n" +
                        "&cAttenzione: Ogni progresso sul tuo account Bedrock non verrà trasferito!\n" +
                        "&cSe cambi idea puoi sempre digitare &6/unlinkaccount&c per tornare indietro."
        ),
        BEDROCK_USAGE("&cInizia il processo con la Java Edition! Utilizzo: /linkaccount <gamertag>"),
        LINK_REQUEST_EXPIRED("&cIl codice inserito è scaduto! Digita &6/linkaccount&c di nuovo sul tuo account Java"),
        LINK_REQUEST_COMPLETED("Il tuo account è stato collegato con successo a %s!\nSe vuoi annullare questa cosa digita /unlinkaccount"),
        LINK_REQUEST_ERROR("&cC'è stato un errore durante il collegamento. " + CommonMessage.CHECK_CONSOLE),
        INVALID_CODE("&cCodice non valido! Per favore controlla il codice o digita nuovamente &6/linkaccount&c sul tuo account Java."),
        NO_LINK_REQUESTED("&cQuesto account non ha richiesto nessun collegamento! Per favore entra con il tuo account Java e richiedilo con &6/linkaccount"),
        LINK_REQUEST_DISABLED("&cCollegamento degli account non attivato su questo server.");

        @Getter private final String message;

        Message(String message) {
            this.message = message.replace('&', COLOR_CHAR);
        }
    }
}
