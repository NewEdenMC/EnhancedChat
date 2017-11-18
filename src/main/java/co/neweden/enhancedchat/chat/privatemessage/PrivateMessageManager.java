package co.neweden.enhancedchat.chat.privatemessage;

import co.neweden.enhancedchat.EnhancedChat;
import co.neweden.enhancedchat.StringEval;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.HashMap;
import java.util.Map;

public class PrivateMessageManager {

    private Map<CommandSender, CommandSender> lastMessageFrom = new HashMap<>();

    public PrivateMessageManager() {
        new PrivateMessageCommand("msg", "pm", "m", "tell", "t", "whisper");
        new PrivateMessageCommand("reply", "r");
    }

    public CommandSender getWhoSentLastMessage(CommandSender to) {
        return lastMessageFrom.get(to);
    }

    public void sendPrivateMessage(CommandSender from, CommandSender to, String message) {
        lastMessageFrom.put(to, from);

        String name = from.getName();
        if (from instanceof ProxiedPlayer)
            name = ((ProxiedPlayer) from).getDisplayName();

        processPrivateMessage(from, name, to, message);
    }

    public void sendPrivateMessage(String from, CommandSender to, String message) {
        processPrivateMessage(null, from, to, message);
    }

    private void processPrivateMessage(CommandSender from, String fromName, CommandSender to, String message) {
        String toName = to.getName();
        if (to instanceof ProxiedPlayer)
            toName = ((ProxiedPlayer) to).getDisplayName();

        if (!EnhancedChat.getConfig().getBoolean("chat.enabled", false)) {
            EnhancedChat.getLogger().warning("Tried to send a Private Message from " + fromName + " to " + toName + " when the Chat feature is disabled.");
            return;
        }

        String messageFormat = EnhancedChat.getConfig().getString("chat.private_messaging.message_format", "&c[%fromDisplayName% -> %toDisplayName%] &f%message%");
        String meFormat = EnhancedChat.getConfig().getString("chat.private_messaging.me_format", "&7&ome&c");
        if (from != null) {
            Map<String, String> tokens = new HashMap<>();
            tokens.put("%toDisplayName%", toName);
            tokens.put("%message%", message);
            TextComponent processedMessage = EnhancedChat.evalMessage(messageFormat, tokens)
                    .addToken("%fromDisplayName%", EnhancedChat.evalMessage(meFormat))
                    .getTextComponent();
            from.sendMessage(processedMessage);
        }

        Map<String, String> tokens = new HashMap<>();
        tokens.put("%fromDisplayName%", fromName);
        tokens.put("%message%", message);
        TextComponent processedMessage = EnhancedChat.evalMessage(messageFormat, tokens)
                .addToken("%toDisplayName%", EnhancedChat.evalMessage(meFormat))
                .getTextComponent();
        to.sendMessage(processedMessage);
    }

}
