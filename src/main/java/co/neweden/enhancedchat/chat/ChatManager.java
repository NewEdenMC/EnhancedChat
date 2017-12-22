package co.neweden.enhancedchat.chat;

import co.neweden.enhancedchat.EnhancedChat;
import co.neweden.enhancedchat.chat.commands.ChatCommands;
import co.neweden.enhancedchat.chat.commands.EmoteCommand;
import co.neweden.enhancedchat.chat.commands.QuickMessageCommand;
import co.neweden.enhancedchat.chat.privatemessage.PrivateMessageManager;
import co.neweden.enhancedchat.tokens.CustomTokens;
import co.neweden.enhancedchat.tokens.Token;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.util.*;

public class ChatManager {

    private static Collection<Channel> channels = new HashSet<>();
    private static Map<ProxiedPlayer, Channel> talkingIn = new HashMap<>();
    private static EventListener eventHandler;
    private static PrivateMessageManager pmm;
    private static boolean discordEnabled = false;
    private static Channel discordStatusChannel = null;
    private static DiscordBot discordBot = null;
    private static Token displayNameOverrideToken;

    public ChatManager() {
        channels.clear();
        talkingIn.clear();
        Configuration config = EnhancedChat.getConfig().getSection("chat");
        if (!config.getBoolean("enabled", false)) return;

        displayNameOverrideToken = CustomTokens.getToken(config.getString("display_name_override_token", null));

        Iterator<String> keys = new LinkedList<>(config.getSection("channels").getKeys()).descendingIterator();
        while (keys.hasNext()) {
            String key = keys.next();
            Configuration chConfig = config.getSection("channels." + key);
            if (!chConfig.getBoolean("enabled", true)) return;
            Channel channel = new Channel(key, chConfig, config.getSection("defaults"));
            channels.add(channel);
            new QuickMessageCommand(channel);
        }

        if (eventHandler != null)
            ProxyServer.getInstance().getPluginManager().unregisterListener(eventHandler);
        eventHandler = new EventListener();
        new ChatCommands(); new EmoteCommand();
        pmm = new PrivateMessageManager();

        discordEnabled = config.getBoolean("discord_integration.enabled", false);
        discordStatusChannel = null;
        if (discordBot != null) {
            discordBot.unload();
            discordBot = null;
        }
        if (discordEnabled) {
            discordStatusChannel = getChannel(EnhancedChat.getConfig().getString("discord_status_messages.message_channel", ""));
            discordBot = new DiscordBot();
        }
    }

    public static Collection<Channel> getChannels() { return Collections.unmodifiableCollection(channels); }

    public static Channel getChannel(String key) {
        String machineKey = key.toUpperCase();
        Optional<Channel> opt = channels.stream()
                .filter(e -> e.getMachineName().equals(machineKey) || e.getMachineShortName().equals(machineKey))
                .findFirst();
        return opt.isPresent() ? opt.get() : null;
    }

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

    public static void sendMessageToActiveChannel(ProxiedPlayer from, Message.Format formatting, String message) {
        Channel activeChannel = ChatManager.getActiveChannel(from);
        if (activeChannel != null)
            activeChannel.sendMessage(from, Message.Source.PLAYER, formatting, message);
        else
            from.sendMessage(new ComponentBuilder("Your chat message could not be sent as you are not talking in any channel, try joining or setting your active channel: use ").color(ChatColor.RED).append("/ch list").color(ChatColor.GOLD).append(" to see a list of channels, ").color(ChatColor.RED).append("/ch NAME").color(ChatColor.GOLD).append(" to join and start talking in a channel").color(ChatColor.RED).create());
    }

    public static PrivateMessageManager getPrivateMessageManager() { return pmm; }

    public static boolean isDiscordEnabled() { return discordEnabled; }

    public static Channel getDiscordStatusChannel() { return discordStatusChannel; }

    public static DiscordBot getDiscordBot() { return discordBot; }

    public static Token getDisplayNameOverrideToken() { return displayNameOverrideToken; }

}
