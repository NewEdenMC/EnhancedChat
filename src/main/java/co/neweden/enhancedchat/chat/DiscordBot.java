package co.neweden.enhancedchat.chat;

import co.neweden.enhancedchat.EnhancedChat;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.util.logging.Level;

public class DiscordBot extends ListenerAdapter {

    private JDA jda;

    DiscordBot() {
        if (!ChatManager.isDiscordEnabled()) return;

        String botToken = EnhancedChat.getConfig().getString("chat.discord_integration.bot_token", "");
        if (botToken.equals("")) {
            EnhancedChat.getLogger().warning("Discord Bot will not be loaded as bot_token value is empty");
            return;
        }
        try {
            jda = new JDABuilder(AccountType.BOT)
                    .setAudioEnabled(false)
                    .setAutoReconnect(true)
                    .setBulkDeleteSplittingEnabled(false)
                    .addEventListener(this)
                    .setToken(botToken)
                    .buildAsync();
        } catch (LoginException | RateLimitedException e) {
            EnhancedChat.getLogger().log(Level.SEVERE, "An exception occurred while trying to connect to Discord: " + e.getMessage(), e);
        }
        EnhancedChat.getLogger().log(Level.INFO, "Connected to Discord Bot");
    }

    public void unload() {
        if (jda == null) return;
        jda.getRegisteredListeners().forEach(e -> jda.removeEventListener(e));
        jda.shutdown();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getJDA().getSelfUser().equals(event.getAuthor())) return;

        if (event.isFromType(ChannelType.PRIVATE)) {
            event.getAuthor().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage("Sorry I am only a Bot and I have not been programmed to respond to private messages").queue());
            return;
        }

        if (event.getMember() == null) return;

        for (Channel channel : ChatManager.getChannels()) {
            if (channel.getDiscordChannelID() == event.getChannel().getIdLong())
                channel.sendMessage(
                        event.getMember().getEffectiveName(),
                        Message.Source.DISCORD,
                        Message.Format.NORMAL,
                        event.getMessage().getContent());
        }
    }

}
