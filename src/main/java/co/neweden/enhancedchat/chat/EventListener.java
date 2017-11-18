package co.neweden.enhancedchat.chat;

import co.neweden.enhancedchat.EnhancedChat;
import co.neweden.enhancedchat.Messages;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class EventListener implements Listener {

    protected EventListener() {
        ProxyServer.getInstance().getPluginManager().registerListener(EnhancedChat.getPlugin(), this);
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        Iterator<Channel> channels = new LinkedList<>(ChatManager.getChannels()).descendingIterator();
        Channel lastJoined = null;
        while (channels.hasNext()) {
            Channel channel = channels.next();
            if (channel.initialJoinPlayer(event.getPlayer()))
                lastJoined = channel;
        }
        if (lastJoined != null)
            ChatManager.setActiveChannel(event.getPlayer(), lastJoined, true);

        if (ChatManager.isDiscordEnabled()) {
            String message = Messages.getHighestStatusMessage("discord_status_messages.join-messages", event.getPlayer());
            Map<String, String> tokens = new HashMap<>();
            tokens.put("%displayName%", event.getPlayer().getDisplayName());
            ChatManager.getDiscordStatusChannel().sendDiscordInfoMessage(
                    EnhancedChat.evalMessage(message, tokens).stripFormatting().urlEval(false, false).getTextComponent().toPlainText()
            );
        }
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer) || event.isCommand()) return;
        ProxiedPlayer sender = (ProxiedPlayer) event.getSender();
        event.setCancelled(true);
        ChatManager.sendMessageToActiveChannel(sender, Message.Format.NORMAL, event.getMessage());
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (ChatManager.isDiscordEnabled()) {
            String message = Messages.getHighestStatusMessage("discord_status_messages.quit-messages", event.getPlayer());
            Map<String, String> tokens = new HashMap<>();
            tokens.put("%displayName%", event.getPlayer().getDisplayName());
            ChatManager.getDiscordStatusChannel().sendDiscordInfoMessage(
                    EnhancedChat.evalMessage(message, tokens).stripFormatting().urlEval(false, false).getTextComponent().toPlainText()
            );
        }
    }

}
