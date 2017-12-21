package co.neweden.enhancedchat.playerdata;

import co.neweden.enhancedchat.EnhancedChat;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;

import java.util.*;
import java.util.logging.Level;

public class PlayerData implements Listener {

    private static Map<String, Group> groups = new HashMap<>();
    private static Map<ProxiedPlayer, TreeSet<Group>> playerGroupsCache = new HashMap<>();

    public PlayerData() {
        groups.clear();
        Configuration config = EnhancedChat.getConfig().getSection("groups");
        for (String key : config.getKeys()) {
            String machineName = key.toLowerCase();
            int rank = config.getInt(key + ".rank", 0);
            groups.put(machineName, new Group(machineName, key, rank));
        }
        playerGroupsCache.clear();
        ProxyServer.getInstance().getPlayers().forEach(PlayerData::cachePlayer);
        ProxyServer.getInstance().getPluginManager().registerListener(EnhancedChat.getPlugin(), this);
    }

    public static void rebuildPlayerGroupCache() {
        playerGroupsCache.clear();
        ProxyServer.getInstance().getPlayers().forEach(PlayerData::cachePlayer);
        EnhancedChat.getLogger().info("Build Player Groups Cache");
    }

    /**
     * Will search current online players for a UUID corresponding to the name provided,
     * if none is found and a Database Connection has been setup it tries to search the database for known UUIDs.
     * As a result of only searching known UUIDs this method can't match UUIDs of players who have never connected
     * or last connected before Enhanced Chat was first setup.
     *
     * @param username the username to search for
     * @return a UUID matching the username provided or null if nothing could be found
     */
    public static UUID getUUIDFromName(String username) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(username);
        return player != null ? player.getUniqueId() : null;
        // todo: add DB lookup
    }

    public static Collection<Group> getGroups() {
        return Collections.unmodifiableCollection(groups.values());
    }

    public static Group getGroup(String name) { return groups.get(name.toLowerCase()); }

    /**
     * Will return a Collection containing the names of the groups a given Player is apart of.  The Collection is
     * backed by a TreeSet which is ordered by the Rank of each Group from the lowest to the highest.
     *
     * Groups are defined in the config.yml file, and a player is part of a group if they have the permission node
     * that is associated with a given group.
     *
     * @param player The player you want to get groups for
     * @return a Unmodifiable Collection of groups that the player is apart of, or an empty Collection if the player
     * is not apart of any groups
     */
    public static Collection<Group> getGroupsForPlayer(ProxiedPlayer player) {
        Collection<Group> groups = playerGroupsCache.get(player);
        if (groups != null)
            return Collections.unmodifiableCollection(groups);
        else
            return Collections.unmodifiableCollection(new LinkedHashSet<>());
    }

    @EventHandler(priority = 4)
    public void onPostLogin(PostLoginEvent event) { cachePlayer(event.getPlayer()); }

    @EventHandler(priority = 6)
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        playerGroupsCache.remove(event.getPlayer());
    }

    private static void cachePlayer(ProxiedPlayer player) {
        TreeSet<Group> playerGroups = playerGroupsCache.get(player);
        if (playerGroups == null) {
            playerGroups = new TreeSet<>();
            playerGroupsCache.put(player, playerGroups);
        }

        for (String perm : player.getPermissions()) {
            String[] parts = perm.split("\\.");
            if (parts.length != 3 || !parts[0].equalsIgnoreCase("enhancedchat") || !parts[1].equalsIgnoreCase("hasgroup")) continue;

            Group group = getGroup(parts[2]);
            if (group == null) {
                EnhancedChat.getLogger().warning("Found permission node \"" + perm + "\" for player \"" + player.getName() + "\" however there is no group with the name \"" + parts[2] + "\", this permission node will be skipped.");
                continue;
            }
            playerGroups.add(group);
        }
    }

}
