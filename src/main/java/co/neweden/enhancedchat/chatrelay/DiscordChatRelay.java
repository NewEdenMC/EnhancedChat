package co.neweden.enhancedchat.chatrelay;

import co.neweden.enhancedchat.EnhancedChat;
import co.neweden.enhancedchat.Messages;
import co.neweden.enhancedchat.StringEval;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class DiscordChatRelay extends ListenerAdapter implements Listener {

    private String herochatDelimiter;
    private DiscordBot bot;

    public DiscordChatRelay() {
        if (!EnhancedChat.getConfig().getBoolean("discord_chat_relay.enabled", false)) return;

        ProxyServer.getInstance().getPluginManager().registerListener(EnhancedChat.getPlugin(), this);

        herochatDelimiter = EnhancedChat.getConfig().getString("discord_chat_relay.herochat_delimiter", ":");
        herochatDelimiter = ChatColor.translateAlternateColorCodes('&', herochatDelimiter);
        bot = new DiscordBot(this);
    }

    public DiscordBot getDiscordBotWrapper() { return bot; }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equalsIgnoreCase("BungeeChat"))
            return; // We only want HerochatBungeeBridge messages

        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        ChannelInfo info = bot.getChannelInfo(in.readUTF());
        if (info == null) return;

        String message = in.readUTF();
        String[] parts = message.split(herochatDelimiter, 2);
        if (parts.length < 2) {
            EnhancedChat.getLogger().warning("Possibly invalid message received on BungeeChat Plugin Messaging Channel, message will not be sent to Discord: " + message);
            return;
        }
        bot.sendUserMessage(info, parts[1], in.readUTF());
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        if (EnhancedChat.getConfig().getBoolean("discord_chat_relay.join-messages.enabled", false)) {
            String message = Messages.getHighestStatusMessage("discord_chat_relay.join-messages", event.getPlayer());
            Map<String, String> tokens = new HashMap<>();
            tokens.put("%displayName%", event.getPlayer().getDisplayName());
            bot.sendInfoMessage(bot.getMainChannel(), EnhancedChat.evalMessage(message, tokens).stripFormatting().urlEval(false, false).getTextComponent().toPlainText());
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (EnhancedChat.getConfig().getBoolean("discord_chat_relay.quit-messages.enabled", false)) {
            String message = Messages.getHighestStatusMessage("discord_chat_relay.quit-messages", event.getPlayer());
            Map<String, String> tokens = new HashMap<>();
            tokens.put("%displayName%", event.getPlayer().getDisplayName());
            bot.sendInfoMessage(bot.getMainChannel(), EnhancedChat.evalMessage(message, tokens).stripFormatting().urlEval(false, false).getTextComponent().toPlainText());
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            event.getPrivateChannel().sendMessage("Sorry I am only a Bot and I have not been programmed to respond to private messages");
            return;
        }

        for (ChannelInfo ci : bot.getChannels()) {
            if (ci.discord_channel_id != event.getChannel().getIdLong()) continue;
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Forward");
            out.writeUTF("ALL");
            out.writeUTF("BungeeChat");
            try {
                ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
                DataOutputStream msgout = new DataOutputStream(msgbytes);
                msgout.writeUTF(ci.herochat_channel_name);
                msgout.writeUTF(event.getMessage().getContent());
            } catch (IOException e) {
                EnhancedChat.getLogger().log(Level.SEVERE, "IOException thrown while constructing Byte Array to send Discord Chat Message to all servers.", e);
            }
        }
    }

}
