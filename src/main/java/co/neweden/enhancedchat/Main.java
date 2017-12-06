package co.neweden.enhancedchat;

import co.neweden.enhancedchat.chat.ChatManager;
import co.neweden.enhancedchat.tokens.CustomTokens;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class Main extends Plugin {

    private Configuration config;

    @Override
    public void onEnable() {
        EnhancedChat.plugin = this;
        load();
    }

    @Override
    public void onDisable() {
        if (ChatManager.getDiscordBot() != null)
            ChatManager.getDiscordBot().unload();
        if (EnhancedChat.isDBConnected()) {
            try {
                EnhancedChat.getDB().close();
            } catch (SQLException e) {
                getLogger().log(Level.WARNING, "Could not close Database Connection, the current operation will still continue but you should investigate this.", e);
            }
        }
        EnhancedChat.db = null;
    }

    public void reload() {
        onDisable();
        getProxy().getPluginManager().unregisterCommands(this);
        getProxy().getPluginManager().unregisterListeners(this);
        getProxy().getScheduler().cancel(this);
        load();
    }

    private void load() {
        EnhancedChat.formattedChat.clear();
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdir();
                setupDefaultFile("chat-motd.txt");

                new File(getDataFolder(), "cmd").mkdir();
                setupDefaultFile("cmd" + File.separator + "colours.txt");
                setupDefaultFile("cmd" + File.separator + "ping.txt");
            }
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(setupDefaultFile("config.yml"));
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
        if (config.getBoolean("custom_tokens.enabled", false)) {
            if (loadDBConnection())
                new CustomTokens();
            else
                getLogger().warning("Custom Tokens will not be enabled as a Database Connection could not be established.");
        }
        new Messages(); new Commands(); new ChatManager();
        loadDynamicCommands();
        EnhancedChat.startUpLoad = false;
    }

    private boolean loadDBConnection() {
        String host = getConfig().getString("mysql.host", null);
        int port = getConfig().getInt("mysql.port", -1);
        String database = getConfig().getString("mysql.database", null);
        if (host == null || port == -1 || database == null) {
            getLogger().log(Level.INFO, "No database information received from config.");
            return false;
        }

        String url = String.format("jdbc:mysql://%s:%s/%s?autoReconnect=true", host, port, database);

        try {
            EnhancedChat.db = DriverManager.getConnection(url, getConfig().getString("mysql.user", ""), getConfig().getString("mysql.password", ""));
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "An SQLException occurred while trying to connect to the database.", e);
            return false;
        }
        getLogger().log(Level.INFO, "Connected to MySQL Database");
        return true;
    }

    private void loadDynamicCommands() {
        for (String key : getConfig().getSection("commands").getKeys()) {
            String command = stripCommandSlash(key);
            String fileName = getConfig().getString("commands." + key + ".file", "");
            List<String> aliases = new ArrayList<>();
            for (String alias : getConfig().getStringList("commands." + key + ".aliases")) {
                aliases.add(stripCommandSlash(alias));
            }
            new DynamicCommandHandler(command, fileName, aliases.toArray(new String[0]));
            getLogger().info("Command \'" + command + "\' registered using file \'" + fileName + (aliases.isEmpty() ? "\'" : "\' with aliases: " + aliases.toString()));
        }
    }

    private String stripCommandSlash(String command) {
        // If command starts with a "/" remove it
        return command.substring(0, 1).equals("/") ? command.substring(1, command.length()) : command;
    }

    private File setupDefaultFile(String name) throws IOException {
        File file = new File(getDataFolder(), name);
        if (!file.exists()) {
            try (InputStream is = getResourceAsStream(name.replace('\\', '/'));
                 OutputStream os = new FileOutputStream(file)) {
                ByteStreams.copy(is, os);
            }
        }
        return file;
    }

    public Configuration getConfig() { return config; }

}
