package co.neweden.enhancedchat.tokens;

import co.neweden.enhancedchat.EnhancedChat;
import co.neweden.enhancedchat.StringEval;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class Token {

    private String name;
    private String machineName;
    private String label;
    private boolean playersEnabled;
    private boolean groupsEnabled;
    private Map<UUID, String> playersCache = new HashMap<>();
    private Map<String, String> groupsCache = new HashMap<>();

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

    public StringEval getValue(ProxiedPlayer player) {
        String value = getRawValue(player);
        if (value == null) return null;
        return new StringEval(value);
    }

    public String getRawValue(ProxiedPlayer player) {
        // When checking playersCache and groupsCache we specifically check for the key and if the value is null
        // as sometimes the cache may contain the key but the value may be null
        String pValue = playersCache.get(player.getUniqueId());
        if (playersCache.containsKey(player.getUniqueId()) && pValue != null)
            return pValue;

        String group = null; // add groups later

        // Last attempt to check the cache
        String gValue = groupsCache.get(group);
        // If no cached value for player now we check group cache
        if (gValue != null)
            return gValue;

        // We don't need to start doing DB Queries if the last attempt to check the cache did have a key but returned null
        if (groupsCache.containsKey(group)) return null;

        // The cache has nothing that is relevant, populate cache from database and return a value
        String dbPValue = getValueFromDB("players", "uuid", player.getUniqueId().toString(), "Players");
        playersCache.put(player.getUniqueId(), dbPValue);
        if (dbPValue != null) return dbPValue;

        String dbGValue = getValueFromDB("groups", "name", group, "Groups");
        playersCache.put(player.getUniqueId(), dbGValue);
        return dbGValue;
    }

    private String getValueFromDB(String tokenValueType, String keyColumn, String keyValue, String tokenValueTypeHuman) {
        String table = "tokens_" + tokenValueType;
        String column = getMachineName();
        try {
            PreparedStatement st = EnhancedChat.getDB().prepareStatement("SELECT " + column + " FROM `" + table + "` WHERE " + keyColumn + "=?;");
            st.setString(1, keyValue);
            ResultSet rs = st.executeQuery();
            if (!rs.isBeforeFirst()) return null;
            rs.next();
            return rs.getString(column);
        } catch (SQLException e) {
            EnhancedChat.getLogger().log(Level.SEVERE, "A SQL Exception occurred while trying to get the " + tokenValueTypeHuman + " Token Value for " + tokenValueTypeHuman + " '" + keyValue + "', for Token '" + getName() + "', in table '" + table + "', at column '" + getMachineName() + "'");
        }
        return null;
    }

    public boolean setPlayerValue(ProxiedPlayer player, String value) {
        try {
            PreparedStatement st = EnhancedChat.getDB().prepareStatement("INSERT INTO `tokens_players` (uuid, " + getMachineName() + ") VALUES (?, ?) ON DUPLICATE KEY UPDATE " + getMachineName() + "=?");
            st.setString(1, player.getUniqueId().toString());
            st.setString(2, value);
            st.setString(3, value);
            st.executeUpdate();
        } catch (SQLException e) {
            EnhancedChat.getLogger().log(Level.SEVERE, "An SQL Exception occurred while setting player token '" + getName() + "' to '" + value + "' for player " + player.getUniqueId());
            return false;
        }
        if (playersCache.containsKey(player.getUniqueId()))
            playersCache.put(player.getUniqueId(), value);
        return true;
    }

    public void setGroupValue(String group, String value) {
        try {
            PreparedStatement st = EnhancedChat.getDB().prepareStatement("INSERT INTO `tokens_groups` (name, " + getMachineName() + ") VALUES (?, ?) ON DUPLICATE KEY UPDATE " + getMachineName() + "=?");
            st.setString(1, group);
            st.setString(2, value);
            st.setString(3, value);
            st.executeUpdate();
        } catch (SQLException e) {
            EnhancedChat.getLogger().log(Level.SEVERE, "An SQL Exception occurred while setting group token '" + getName() + "' to '" + value + "' for group '" + group + "'");
        }
        if (groupsCache.containsKey(group))
            groupsCache.put(group, value);
    }

}
