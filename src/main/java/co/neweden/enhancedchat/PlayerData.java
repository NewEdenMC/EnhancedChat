package co.neweden.enhancedchat;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class PlayerData {

    private static String groupsSource;

    static void init() {
        groupsSource = EnhancedChat.getConfig().getString("groups_source", "bungee");
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
