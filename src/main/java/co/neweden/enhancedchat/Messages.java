package co.neweden.enhancedchat;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;

import java.util.HashMap;
import java.util.Map;

public class Messages implements Listener {

    public Messages() {
        ProxyServer.getInstance().getPluginManager().registerListener(EnhancedChat.getPlugin(), this);
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        String chat_motd_file = EnhancedChat.getPlugin().getConfig().getString("chat-motd-file", null);
        if (chat_motd_file == null) return;
        EnhancedChat.sendMessageFromPath(event.getPlayer(), chat_motd_file, "An error has occurred while trying to load the Chat MOTD File, please inform a member of staff.");

        if (EnhancedChat.getPlugin().getConfig().getBoolean("join-messages.enabled", false)) {
            String message = getHighestStatusMessage("join-messages", event.getPlayer());
            Map<String, String> tokens = new HashMap<>();
            tokens.put("%displayName%", event.getPlayer().getDisplayName());
            ProxyServer.getInstance().broadcast(EnhancedChat.evalMessage(message, tokens).getTextComponent());
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (EnhancedChat.getPlugin().getConfig().getBoolean("quit-messages.enabled", false)) {
            String message = getHighestStatusMessage("quit-messages", event.getPlayer());
            Map<String, String> tokens = new HashMap<>();
            tokens.put("%displayName%", event.getPlayer().getDisplayName());
            ProxyServer.getInstance().broadcast(EnhancedChat.evalMessage(message, tokens).getTextComponent());
        }
    }

    private String getHighestStatusMessage(String configSection, ProxiedPlayer player) {
        Configuration configGroups = EnhancedChat.getPlugin().getConfig().getSection(configSection + ".groups");
        String def = EnhancedChat.getPlugin().getConfig().getString(configSection + ".default", "");

        if (configGroups.getKeys().size() == 0)
            return def;

        String groupToUse = null;

        // We are looping through each config group in the order it is entered in the config
        // groupToUse will end up being the last group checked that the player has
        for (String configGroup : configGroups.getKeys()) {
            if (player.getGroups().contains(configGroup))
                groupToUse = configGroup;
        }

        if (groupToUse == null)
            return def;
        else
            return configGroups.getString(groupToUse, "");
    }

}
