package co.neweden.enhancedchat.tokens;

import co.neweden.enhancedchat.EnhancedChat;
import net.md_5.bungee.config.Configuration;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class CustomTokens {

    private static Map<String, Token> tokens = new HashMap<>();

    public CustomTokens() {
        tokens.clear();
        if (!EnhancedChat.getConfig().getBoolean("custom_tokens.enabled", false) || !EnhancedChat.isDBConnected() || !setupDB()) return;
        Configuration config = EnhancedChat.getConfig().getSection("custom_tokens.tokens");
        if (config == null) return;

        for (String name : config.getKeys()) {
            if (name.length() > 64) {
                EnhancedChat.getLogger().warning("Token name '" + name + "' is to long, name cannot be longer than 64 characters, skipping to next token...");
                continue;
            }

            Configuration tokenConfig = config.getSection(name);
            boolean playersEnabled = tokenConfig.getBoolean("players_enabled", false);
            boolean groupsEnabled = tokenConfig.getBoolean("groups_enabled", false);
            if (!playersEnabled && !groupsEnabled) continue;

            Token token = new Token(name, playersEnabled, groupsEnabled);

            try {
                if (playersEnabled)
                    EnhancedChat.getDB().createStatement().execute("ALTER TABLE `tokens_players` ADD COLUMN `" + token.getMachineName() + "` VARCHAR(45) NULL;");
            } catch (SQLException e) {
                if (!e.getSQLState().equals("42S21")) {
                    EnhancedChat.getLogger().log(Level.SEVERE, "An SQL Exception occurred while trying to add the Players Token Column ('" + token.getMachineName() + "' in table 'tokens_players') for Token '" + token.getName() + "'", e);
                    continue;
                }
            }
            try {
                if (groupsEnabled)
                    EnhancedChat.getDB().createStatement().execute("ALTER TABLE `tokens_groups` ADD COLUMN `" + token.getMachineName() + "` VARCHAR(45) NULL;");
            } catch (SQLException e) {
                if (!e.getSQLState().equals("42S21")) {
                    EnhancedChat.getLogger().log(Level.SEVERE, "An SQL Exception occurred while trying to add the Groups Token Column ('" + token.getMachineName() + "' in table 'tokens_groups') for Token '" + token.getName() + "'", e);
                    continue;
                }
            }

            tokens.put(name, token);

            String regFor = "nothing";
            if (playersEnabled && groupsEnabled) regFor = "Players and Groups";
            else if (playersEnabled) regFor = "Players";
            else if (groupsEnabled) regFor = "Groups";

            EnhancedChat.getLogger().info("Custom Token '" + name + "' has been registered for " + regFor);
        }
    }

    private static boolean setupDB() {
        try {
            EnhancedChat.getDB().createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS `tokens_players` (\n" +
                    "  `uuid` VARCHAR(36) NOT NULL,\n" +
                    "  PRIMARY KEY (`uuid`)\n" +
                    ");\n"
            );
            EnhancedChat.getDB().createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS `tokens_groups` (\n" +
                    "  `name` VARCHAR(64) NOT NULL,\n" +
                    "  PRIMARY KEY (`name`)\n" +
                    ");\n"
            );
        } catch (SQLException e) {
            EnhancedChat.getLogger().log(Level.SEVERE, "Unable to setup database tables for Custom Tokens", e);
            return false;
        }
        return true;
    }

    public static Collection<Token> getTokens() {
        return Collections.unmodifiableCollection(tokens.values());
    }

    public static Token getToken(String name) { return tokens.get(name); }

}
