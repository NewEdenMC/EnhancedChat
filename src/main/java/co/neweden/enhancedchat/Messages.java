package co.neweden.enhancedchat;

import co.neweden.enhancedchat.playerdata.Group;
import co.neweden.enhancedchat.playerdata.PlayerData;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Messages implements Listener {

    private int autoMessageCounter;
    private final List<TextComponent> autoMessages = new ArrayList<>();

    public Messages() {
        ProxyServer.getInstance().getPluginManager().registerListener(EnhancedChat.getPlugin(), this);
        startAnnouncements();
    }

    @EventHandler(priority = 6)
    public void onPostLogin(PostLoginEvent event) {
        if (EnhancedChat.getConfig().getBoolean("chat-motd.enabled", false)) {
            String chat_motd_file = EnhancedChat.getConfig().getString("chat-motd.file", "");
            EnhancedChat.sendMessageFromPath(event.getPlayer(), chat_motd_file, "An error has occurred while trying to load the Chat MOTD File, please inform a member of staff.");
        }

        if (EnhancedChat.getConfig().getBoolean("join-messages.enabled", false)) {
            String message = getHighestStatusMessage("join-messages", event.getPlayer());
            Map<String, String> tokens = new HashMap<>();
            tokens.put("%displayName%", event.getPlayer().getDisplayName());
            ProxyServer.getInstance().broadcast(EnhancedChat.evalMessage(message, tokens).getTextComponent());
        }
    }

    @EventHandler(priority = 6)
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (EnhancedChat.getConfig().getBoolean("quit-messages.enabled", false)) {
            String message = getHighestStatusMessage("quit-messages", event.getPlayer());
            Map<String, String> tokens = new HashMap<>();
            tokens.put("%displayName%", event.getPlayer().getDisplayName());
            ProxyServer.getInstance().broadcast(EnhancedChat.evalMessage(message, tokens).getTextComponent());
        }
    }

    public static String getHighestStatusMessage(String configSection, ProxiedPlayer player) {
        Configuration configGroups = EnhancedChat.getConfig().getSection(configSection + ".groups");
        String def = EnhancedChat.getConfig().getString(configSection + ".default", "");

        if (configGroups.getKeys().size() == 0)
            return def;

        Group groupToUse = null;

        // We are looping through each config group in the order it is entered in the config
        // groupToUse will end up being the last group checked that the player has
        Collection<Group> playerGroups = PlayerData.getGroupsForPlayer(player);
        for (String configGroup : configGroups.getKeys()) {
            Group testGroup = PlayerData.getGroup(configGroup);
            if (testGroup != null && playerGroups.contains(testGroup))
                groupToUse = testGroup;
        }

        if (groupToUse == null)
            return def;
        else
            return configGroups.getString(groupToUse.getName(), "");
    }

    public void startAnnouncements() {
        if (!EnhancedChat.getConfig().getBoolean("auto_messages.enabled", false))
            return; // If not enabled in the config, don't do anything

        long delay = EnhancedChat.getConfig().getLong("auto_messages.delay", 600);
        final String prefix = EnhancedChat.getConfig().getString("auto_messages.prefix", "");

        autoMessageCounter = 0;
        autoMessages.clear();
        EnhancedChat.getConfig().getStringList("auto_messages.messages").forEach(msg -> {
            autoMessages.add(new StringEval(prefix + msg).getTextComponent());
        });

        EnhancedChat.getLogger().info("Loaded " + autoMessages.size() + " Auto Messages with a delay of " + delay + " second(s).");
        if (autoMessages.size() <= 0) return;

        ProxyServer.getInstance().getScheduler().schedule(EnhancedChat.getPlugin(), new Runnable() {
            @Override
            public void run() {
                ProxyServer.getInstance().broadcast(autoMessages.get(autoMessageCounter));
                if (autoMessageCounter < autoMessages.size() - 1)
                    autoMessageCounter++;
                else
                    autoMessageCounter = 0;
            }
        }, EnhancedChat.startUpLoad ? delay : 0, delay, TimeUnit.SECONDS);
    }

}
