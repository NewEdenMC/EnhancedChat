package co.neweden.enhancedchat.chatrelay;

import co.neweden.enhancedchat.EnhancedChat;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.Configuration;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class DiscordBot {

    private Map<String, ChannelInfo> herochatChannels = new HashMap<>();
    private ChannelInfo mainChannel = null;
    private JDA jda;

    protected DiscordBot(ListenerAdapter jdaListener) {
        String botToken = EnhancedChat.getConfig().getString("discord_chat_relay.bot_token", "");
        loadChannels();
        if (botToken.equals("")) {
            EnhancedChat.getLogger().warning("Discord Bot will not be loaded as bot_token value is empty");
            return;
        }
        try {
            jda = new JDABuilder(AccountType.BOT)
                    .setAudioEnabled(false)
                    .setAutoReconnect(true)
                    .setBulkDeleteSplittingEnabled(false)
                    .addEventListener(jdaListener)
                    .setToken(botToken)
                    .buildAsync();
        } catch (LoginException | RateLimitedException e) {
            EnhancedChat.getLogger().log(Level.SEVERE, "An exception occurred while trying to connect to Discord: " + e.getMessage(), e);
        }
        EnhancedChat.getLogger().log(Level.INFO, "Connected to Discord Bot");
    }

    private void loadChannels() {
        Configuration dcConfig = EnhancedChat.getPlugin().getConfig().getSection("discord_chat_relay.herochat_channels");
        String mainChannel = EnhancedChat.getConfig().getString("discord_chat_relay.main_herochat_channel", "");
        ChannelInfo first = null;

        for (String channelName : dcConfig.getKeys()) {
            Configuration section = dcConfig.getSection(channelName);

            if (!section.getBoolean("enabled", false))
                continue; // if channel is disabled in config, continue

            ChannelInfo chInfo = new ChannelInfo();
            chInfo.herochat_channel_name = channelName;
            chInfo.discord_channel_id = section.getLong("discord_channel_id", 0);
            chInfo.discord_webhook_url = section.getString("webhook_url", "");
            chInfo.herochat_delimiter = section.getString("herochat_delimiter",
                    EnhancedChat.getConfig().getString("discord_chat_relay.herochat_default_delimiter", "&f: ")
            );
            chInfo.herochat_delimiter = ChatColor.translateAlternateColorCodes('&', chInfo.herochat_delimiter);
            chInfo.discord_to_game_format = section.getString("discord_to_game_format",
                    EnhancedChat.getConfig().getString("discord_chat_relay.discord_to_game_default_format", "&b#Discord %displayName%&f: %message%")
            );

            if (mainChannel.equalsIgnoreCase(channelName))
                this.mainChannel = chInfo;

            herochatChannels.put(chInfo.herochat_channel_name, chInfo);
            if (first == null) first = chInfo;
            EnhancedChat.getLogger().info("Discord Chat Relay cached between Herochat Channel \"" + chInfo.herochat_channel_name + "\" and Discord channel \"" + chInfo.discord_channel_id + "\"");
        }

        if (mainChannel.equals("")) this.mainChannel = first;
    }

    public ChannelInfo getMainChannel() { return this.mainChannel; }

    public ChannelInfo getChannelInfo(String heroChannelName) {
        return herochatChannels.get(heroChannelName);
    }

    public Collection<ChannelInfo> getChannels() { return herochatChannels.values(); }

    public void sendInfoMessage(ChannelInfo channel, String message) {
        message = message.replace('\u00A7', '\u0000');
        try {
            sendData(channel, "{\"username\":\"MC Server\",\"embeds\":[{\"description\":\"" + message + "\"}]}");
        } catch (IOException e) { EnhancedChat.getLogger().log(Level.SEVERE, "UOException occurred while trying to send an info message to a Discord channel", e); }
    }

    public void sendUserMessage(ChannelInfo channel, String message, String from) {
        message = message.replace('\u00A7', '\u0000');
        try {
            sendData(channel, "{\"content\":\"" + message + "\",\"username\":\"[MC] " + from + "\"}");
        } catch (IOException e) { EnhancedChat.getLogger().log(Level.SEVERE, "UOException occurred while trying to send a user message to a Discord channel", e); }
    }

    public void unload() {
        if (jda == null) return;
        jda.getRegisteredListeners().forEach(e -> jda.removeEventListener(e));
        jda.shutdown();
    }

    private void sendData(ChannelInfo channel, String params) throws IOException {
        URL obj = new URL(channel.discord_webhook_url);
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
