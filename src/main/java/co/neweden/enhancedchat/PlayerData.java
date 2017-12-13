package co.neweden.enhancedchat;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class PlayerData {

    private static String groupsSource;

    static void init() {
        groupsSource = EnhancedChat.getConfig().getString("groups_source", "bungee");
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
        return ProxyServer.getInstance().getPlayer(username).getUniqueId();
        // todo: add DB lookup
    }

    /**
     * Will return a Collection containing the names of the groups a given Player is apart of, the source set in the
     * config.yml file will determine where the Collection of groups comes from.
     *
     * @param player The player you want to get groups for
     * @return a Collection of group names or an empty Collection if no groups to return
     */
    public static Collection<String> getGroups(ProxiedPlayer player) {
        if (groupsSource.equalsIgnoreCase("bungee")) {
            return player.getGroups();
        }
        return Collections.unmodifiableCollection(new ArrayList<>());
    }

    /**
     * Will return the primary group for the given Player, this is a wrapper for the getGroups method and will usually
     * return the first group name in the Collection.
     *
     * @param player The player you want to get the primary group for
     * @return a String as the name of the group or null if the player doesn't belong to any groups
     */
    public static String getPrimaryGroup(ProxiedPlayer player) {
        Collection<String> groups = getGroups(player);
        if (groups.size() < 1)
            return null;
        else
            return groups.iterator().next();
    }

}
