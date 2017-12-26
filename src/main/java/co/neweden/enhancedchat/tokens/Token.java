package co.neweden.enhancedchat.tokens;

import co.neweden.enhancedchat.EnhancedChat;
import co.neweden.enhancedchat.StringEval;
import co.neweden.enhancedchat.playerdata.Group;
import co.neweden.enhancedchat.playerdata.PlayerData;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public class Token {

    private String name;
    private String machineName;
    private String label;
    private boolean playersEnabled;
    private boolean groupsEnabled;
    Map<UUID, StringEval> playersCache = new HashMap<>();
    Map<Group, StringEval> groupValues = new HashMap<>();

    Token(String name, String label, boolean playersEnabled, boolean groupsEnabled) {
        this.name = name;
        machineName = "token_" + name;
        this.label = label != null ? label : name;
        this.playersEnabled = playersEnabled;
        this.groupsEnabled = groupsEnabled;
    }

    public String getName() { return name; }

    public String getMachineName() { return machineName; }

    public String getLabel() { return label; }

    public boolean isPlayersEnabled() { return playersEnabled; }

    public boolean isGroupsEnabled() { return groupsEnabled; }

    public StringEval getValue(UUID uuid) {
        StringEval value = null;

        // We need to check the cache to see if the player value has already been cached
        if (isPlayersEnabled()) {
            value = playersCache.get(uuid);
            if (value != null)
                return value;

            // If the cache doesn't have the player then we need to query the DB to populate the cache.
            // We have the following check because the cache might have the player but they might simply just have no
            // value (aka "null") as their value.  If they have null and are in the cache we don't need to check the
            // DB because we know that has already been done for this player.
            if (!playersCache.containsKey(uuid))
                value = cachePlayerValueFromDB(uuid);

            // At this point if value is not null we definitely have a player specific value, otherwise we now need
            // to check their highest matching group
            if (value != null)
                return value;
        }

        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(uuid); // todo: need support for offline players
        Group group = null;

        if (isGroupsEnabled() && player != null) {
            Collection<Group> playerGroups = PlayerData.getGroupsForPlayer(player);
            // Loop through each group the player has, ordered by rank, so that the last group that matches will be
            // the player's highest group
            for (Group playerGroup : playerGroups) {
                if (groupValues.get(playerGroup) != null)
                    group = playerGroup;
            }
        }

        if (group != null) {
            // Will not be null if this token has a value for the group selected
            // Last attempt to check the cache
            value = getValueForGroup(group);
        }

        // At this point either we have a value from their group or we don't, both are equally terrifying
        return value;
    }

    private StringEval cachePlayerValueFromDB(UUID uuid) {
        try {
            PreparedStatement st = EnhancedChat.getDB().prepareStatement("SELECT " + getMachineName() + " FROM `tokens_players` WHERE uuid=?;");
            st.setString(1, uuid.toString());
            ResultSet rs = st.executeQuery();
            if (!rs.isBeforeFirst()) return null;
            rs.next();
            String dbValue = rs.getString(1);
            if (dbValue == null) {
                playersCache.put(uuid, null);
                return null;
            }
            StringEval value = new StringEval(dbValue);
            playersCache.put(uuid, value);
            return value;
        } catch (SQLException e) {
            EnhancedChat.getLogger().log(Level.SEVERE, "A SQL Exception occurred while trying to get the Player Token Value for '" + uuid + "', for Token '" + getName() + "', in table 'tokens_players', at column '" + getMachineName() + "'", e);
        }
        return null;
    }

    public StringEval getValueForGroup(Group group) { return groupValues.get(group); }

    public boolean setPlayerValue(UUID uuid, String value) {
        try {
            PreparedStatement st = EnhancedChat.getDB().prepareStatement("INSERT INTO `tokens_players` (uuid, " + getMachineName() + ") VALUES (?, ?) ON DUPLICATE KEY UPDATE " + getMachineName() + "=?");
            st.setString(1, uuid.toString());
            st.setString(2, value);
            st.setString(3, value);
            st.executeUpdate();
        } catch (SQLException e) {
            EnhancedChat.getLogger().log(Level.SEVERE, "An SQL Exception occurred while setting player token '" + getName() + "' to '" + value + "' for player " + uuid);
            return false;
        }
        if (playersCache.containsKey(uuid))
            playersCache.put(uuid, value == null ? null : new StringEval(value));
        return true;
    }

    public boolean setGroupValue(Group group, String value) {
        try {
            PreparedStatement st = EnhancedChat.getDB().prepareStatement("INSERT INTO `tokens_groups` (name, " + getMachineName() + ") VALUES (?, ?) ON DUPLICATE KEY UPDATE " + getMachineName() + "=?");
            st.setString(1, group.getMachineName());
            st.setString(2, value);
            st.setString(3, value);
            st.executeUpdate();
        } catch (SQLException e) {
            EnhancedChat.getLogger().log(Level.SEVERE, "An SQL Exception occurred while setting group token '" + getName() + "' to '" + value + "' for group '" + group + "'");
            return false;
        }
        groupValues.put(group, value == null ? null : new StringEval(value));
        return true;
    }

}
