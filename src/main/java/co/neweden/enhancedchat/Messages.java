package co.neweden.enhancedchat;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.logging.Level;

public class Messages implements Listener {

    public Messages() {
        EnhancedChat.getPlugin().getProxy().getPluginManager().registerListener(EnhancedChat.getPlugin(), this);
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        String chat_motd_file = EnhancedChat.getPlugin().getConfig().getString("chat-motd-file", null);
        if (chat_motd_file == null) return;
        sendMessageFromPath(event.getPlayer(), chat_motd_file, "An error has occurred while trying to load the Chat MOTD File, please inform a member of staff.");
    }

    public boolean sendMessageFromPath(CommandSender sender, String path, String errorMessage) {
        if (path.isEmpty()) return false;
        try {
            sender.sendMessage(EnhancedChat.safeGetFormattedFile(path));
            return true;
        } catch (Exception e) {
            EnhancedChat.getLogger().log(Level.WARNING, e.getMessage(), e);
            sender.sendMessage(new ComponentBuilder(errorMessage).color(ChatColor.RED).create());
            return false;
        }
    }

}
