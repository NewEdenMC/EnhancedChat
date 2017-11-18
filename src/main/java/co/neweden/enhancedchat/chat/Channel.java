package co.neweden.enhancedchat.chat;

import co.neweden.enhancedchat.EnhancedChat;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.util.*;

/**
 * Stores information about and manages a channel
 */

public class Channel {

    private String name;
    private String shortName;
    private String formatGameNormal;
    private String formatGameEmote;
    private Collection<ProxiedPlayer> chatters = new HashSet<>();

    protected Channel(String key, Configuration channel, Configuration defaults) {
        if (!channel.getBoolean("enabled", false)) throw new IllegalArgumentException("EnhancedChat Channel '" + key + "' cannot be created because it is not enabled.");
        name = key;
        shortName = channel.getString("shortName", key);
        formatGameNormal = channel.getString("formatting.game_normal", defaults.getString("formatting.game_normal", "&b#%chShortName% %displayName%&f: %message%"));
        formatGameEmote = channel.getString("formatting.game_emote", defaults.getString("formatting.game_emote", "&b#%chShortName% &e* %displayName% %message%"));

        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            initialJoinPlayer(player);
        }
    }

    public String getName() { return name; }

    public String getShortName() { return shortName; }

    public String getFormatGameNormal() { return formatGameNormal; }

    public String getFormatGameEmote() { return formatGameEmote; }

    protected boolean initialJoinPlayer(ProxiedPlayer player) {
        if (!player.hasPermission("enhancedchat.channel." + getName() + ".autojoin")) return false;
        joinPlayer(player, true);
        return true;
    }

    public void joinPlayer(ProxiedPlayer player) { joinPlayer(player, false); }
    public void joinPlayer(ProxiedPlayer player, boolean silentJoin) {
        if (!canJoin(player)) return;
        chatters.add(player);
        if (!silentJoin)
            player.sendMessage(new ComponentBuilder("You have joined the channel: ").color(ChatColor.GREEN).append(getName()).color(ChatColor.WHITE).create());
    }

    public boolean canJoin(ProxiedPlayer player) {
        return player.hasPermission("enhancedchat.channel." + getName() + ".join");
    }

    public void removePlayer(ProxiedPlayer player) { removePlayer(player, false); }
    public void removePlayer(ProxiedPlayer player, boolean silentLeave) {
        if (ChatManager.getActiveChannel(player).equals(this)) {
            Channel newActive = null;
            Optional<Channel> opt = ChatManager.getChannels().stream().filter(e -> e.getChatters().contains(player)).findFirst();
            if (opt.isPresent())
                newActive = opt.get();
            ChatManager.setActiveChannel(player, newActive);
        }
        chatters.remove(player);
        if (!silentLeave)
            player.sendMessage(new ComponentBuilder("You have left the channel: ").color(ChatColor.AQUA).append(getName()).color(ChatColor.WHITE).create());
    }

    /**
     * Gets a Collection representing ProxiedPlayers who are currently in this channel
     *
     * @return Unmodifiable Collection of type ProxiedPlayer
     */
    public Collection<ProxiedPlayer> getChatters() {
        return Collections.unmodifiableCollection(chatters);
    }

    public void sendMessage(ProxiedPlayer from, MessageType type, String message) { processMessage(from.getDisplayName(), type, message); }

    public void sendMessage(String from, MessageType type, String message) { processMessage(from, type, message); }

    private void processMessage(String fromName, MessageType type, String message) {
        Map<String, String> tokens = new HashMap<>();
        tokens.put("%chShortName%", getShortName());
        tokens.put("%displayName%", fromName);
        tokens.put("%message%", message);
        String format = "";
        switch (type) {
            case GAME_NORMAL: format = getFormatGameNormal(); break;
            case GAME_EMOTE: format = getFormatGameEmote(); break;
        }
        TextComponent out = EnhancedChat.evalMessage(format, tokens).getTextComponent();
        for (ProxiedPlayer player : getChatters()) {
            player.sendMessage(out);
        }
    }

}
