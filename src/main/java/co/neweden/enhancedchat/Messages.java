package co.neweden.enhancedchat;

import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class Messages implements Listener {

    public Messages() {
        EnhancedChat.getPlugin().getProxy().getPluginManager().registerListener(EnhancedChat.getPlugin(), this);
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        String chat_motd_file = EnhancedChat.getPlugin().getConfig().getString("chat-motd-file", null);
        if (chat_motd_file == null) return;
        EnhancedChat.sendMessageFromPath(event.getPlayer(), chat_motd_file, "An error has occurred while trying to load the Chat MOTD File, please inform a member of staff.");
    }

}
