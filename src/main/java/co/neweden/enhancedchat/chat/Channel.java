package co.neweden.enhancedchat.chat;

import co.neweden.enhancedchat.EnhancedChat;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;

/**
 * Stores information about and manages a channel
 */

public class Channel {

    private String name;
    private String shortName;
    private String discordWebhookURL;
    private long discordChannelID;
    private String formatGameNormal;
    private String formatGameEmote;
    private String formatDiscordNormal;
    private Collection<ProxiedPlayer> chatters = new HashSet<>();

    protected Channel(String key, Configuration channel, Configuration defaults) {
        if (!channel.getBoolean("enabled", false)) throw new IllegalArgumentException("EnhancedChat Channel '" + key + "' cannot be created because it is not enabled.");
        name = key;
        shortName = channel.getString("shortName", key);
        discordWebhookURL = channel.getString("discord.webhook_url", "");
        discordChannelID = channel.getLong("discord.channel_id", 0);
        formatGameNormal = channel.getString("formatting.player_normal", defaults.getString("formatting.player_normal", "&b#%chShortName% %displayName%&f: %message%"));
        formatGameEmote = channel.getString("formatting.player_emote", defaults.getString("formatting.player_emote", "&b#%chShortName% &e* %displayName% %message%"));
        formatDiscordNormal = channel.getString("formatting.discord_normal", defaults.getString("formatting.discord_normal", "&b#%chShortName% #Discord %displayName%&f: %message%"));

        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            initialJoinPlayer(player);
        }
    }

    public String getName() { return name; }

    public String getShortName() { return shortName; }

    public String getDiscordWebhookURL() { return discordWebhookURL; }

    public long getDiscordChannelID() { return discordChannelID; }

    public String getFormatGameNormal() { return formatGameNormal; }

    public String getFormatGameEmote() { return formatGameEmote; }

    public String getFormatDiscordNormal() { return formatDiscordNormal; }

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
        chatters.remove(player);
        if (!silentLeave)
            player.sendMessage(new ComponentBuilder("You have left the channel: ").color(ChatColor.RED).append(getName()).color(ChatColor.WHITE).create());

        if (!this.equals(ChatManager.getActiveChannel(player))) return;

        Channel newActive = null;
        Optional<Channel> opt = ChatManager.getChannels().stream().filter(e -> e.getChatters().contains(player)).findFirst();
        if (opt.isPresent())
            newActive = opt.get();
        ChatManager.setActiveChannel(player, newActive, silentLeave);
    }

    /**
     * Gets a Collection representing ProxiedPlayers who are currently in this channel
     *
     * @return Unmodifiable Collection of type ProxiedPlayer
     */
    public Collection<ProxiedPlayer> getChatters() {
        return Collections.unmodifiableCollection(chatters);
    }

    public void sendMessage(ProxiedPlayer from, Message.Source source, Message.Format format, String message) {
        processMessage(from.getDisplayName(), from.getUniqueId(), source, format, message);
    }

    public void sendMessage(String from, Message.Source source, Message.Format format, String message) {
        processMessage(from, null, source, format, message);
    }

    private void processMessage(String fromName, UUID uuid, Message.Source source, Message.Format format, String message) {
        Map<String, String> tokens = new HashMap<>();
        tokens.put("%chShortName%", getShortName());
        tokens.put("%displayName%", fromName);
        tokens.put("%message%", message);
        String formatting = "";
        if (source == Message.Source.PLAYER) {
            switch (format) {
                case NORMAL: formatting = getFormatGameNormal(); break;
                case EMOTE: formatting = getFormatGameEmote(); break;
            }
            sendDiscordUserMessage(fromName, uuid, message);
        } else if (source == Message.Source.DISCORD)
            formatting = getFormatDiscordNormal();

        TextComponent out = EnhancedChat.evalMessage(formatting, tokens).getTextComponent();
        for (ProxiedPlayer player : getChatters()) {
            player.sendMessage(out);
        }
    }

    public void sendDiscordInfoMessage(String message) {
        if (!ChatManager.isDiscordEnabled()) return;
        message = message.replace('\u00A7', '\u0000');
        try {
            processDiscordMessage("{\"username\":\"MC Server\",\"embeds\":[{\"description\":\"" + message + "\"}]}");
        } catch (IOException e) {
            EnhancedChat.getLogger().log(Level.SEVERE, "UOException occurred while trying to send an info message to a Discord channel", e);
        }
    }

    public void sendDiscordUserMessage(String from, UUID uuid, String message) {
        if (!ChatManager.isDiscordEnabled()) return;
        message = message.replace('\u00A7', '\u0000');
        String avatarPart = uuid != null ? ",\"avatar_url\":\"https://crafatar.com/renders/head/" + uuid + "?overlay=true\"" : "";
        try {
            processDiscordMessage("{\"content\":\"" + message + "\",\"username\":\"[MC] " + from + "\"" + avatarPart + "}");
        } catch (IOException e) {
            EnhancedChat.getLogger().log(Level.SEVERE, "UOException occurred while trying to send a user message to a Discord channel", e);
        }
    }

    private void processDiscordMessage(String params) throws IOException {
        URL obj = new URL(getDiscordWebhookURL());
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux i686) Gecko/20071127 Firefox/2.0.0.11");

        // For POST only - START
        con.setDoOutput(true);
        OutputStream os = con.getOutputStream();
        os.write(params.getBytes());
        os.flush();
        os.close();
        // For POST only - END

        if (con.getResponseCode() >= 300) {
            EnhancedChat.getLogger().warning("Discord Web Hook API responded with an unexpected HTTP Status Code: " + con.getResponseCode() + " " + con.getResponseMessage() + "\nRequest was: " + params);
        }
    }

}
