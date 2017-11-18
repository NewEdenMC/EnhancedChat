package co.neweden.enhancedchat.chat;

import co.neweden.enhancedchat.EnhancedChat;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Iterator;
import java.util.LinkedList;

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
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer) || event.isCommand()) return;
        ProxiedPlayer sender = (ProxiedPlayer) event.getSender();
        event.setCancelled(true);
        ChatManager.sendMessageToActiveChannel(sender, MessageType.PLAYER_NORMAL, event.getMessage());
    }

}
