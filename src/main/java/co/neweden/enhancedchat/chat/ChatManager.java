package co.neweden.enhancedchat.chat;

import co.neweden.enhancedchat.EnhancedChat;
import co.neweden.enhancedchat.chat.privatemessage.PrivateMessageManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.util.*;

public class ChatManager {

    private static Map<String, Channel> channels = new LinkedHashMap<>();
    private static Map<ProxiedPlayer, Channel> talkingIn = new HashMap<>();
    private static EventListener eventHandler;
    private static PrivateMessageManager pmm;

    public ChatManager() {
        channels.clear();
        talkingIn.clear();
        Configuration config = EnhancedChat.getConfig().getSection("chat");
        if (!config.getBoolean("enabled", false)) return;

        Iterator<String> keys = new LinkedList<>(config.getSection("channels").getKeys()).descendingIterator();
        while (keys.hasNext()) {
            String key = keys.next();
            Configuration chConfig = config.getSection("channels." + key);
            if (!chConfig.getBoolean("enabled", true)) return;
            channels.put(key, new Channel(key, chConfig, config.getSection("defaults")));
        }

        if (eventHandler != null)
            ProxyServer.getInstance().getPluginManager().unregisterListener(eventHandler);
        eventHandler = new EventListener();
        new ChannelCommands(); new EmoteCommand();
        pmm = new PrivateMessageManager();
    }

    public static Collection<Channel> getChannels() { return Collections.unmodifiableCollection(channels.values()); }

    public static Channel getChannel(String name) { return channels.get(name); }

    public static Channel getActiveChannel(ProxiedPlayer player) {
        return talkingIn.get(player);
    }

    public static void setActiveChannel(ProxiedPlayer player, Channel channel) { setActiveChannel(player, channel, false); }
    public static void setActiveChannel(ProxiedPlayer player, Channel channel, boolean setSilently) {
        if (channel == null) {
            talkingIn.remove(player); return;
        }

        if (!channel.getChatters().contains(player)) return;

        talkingIn.put(player, channel);
        if (!setSilently)
            player.sendMessage(new ComponentBuilder("You are now talking in the channel: ").color(ChatColor.AQUA).append(channel.getName()).color(ChatColor.WHITE).create());
    }

    public static void sendMessageToActiveChannel(ProxiedPlayer from, MessageType type, String message) {
        Channel activeChannel = ChatManager.getActiveChannel(from);
        if (activeChannel != null)
            activeChannel.sendMessage(from, type, message);
        else
            from.sendMessage(new ComponentBuilder("Your chat message could not be sent as you are not in a channel, try joining one: use ").color(ChatColor.RED).append("/ch list").color(ChatColor.GOLD).append(" to see a list of channels and ").color(ChatColor.RED).append("/ch join NAME").color(ChatColor.GOLD).append(" to join a channel.").color(ChatColor.RED).create());
    }

    public static PrivateMessageManager getPrivateMessageManager() { return pmm; }

}
