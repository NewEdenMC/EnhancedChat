package co.neweden.enhancedchat;

import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.util.logging.Level;

public class Main extends Plugin {

    private Configuration config;

    @Override
    public void onEnable() {
        EnhancedChat.plugin = this;
        load();
        new Messages();
    }

    private void load() {
        EnhancedChat.formattedChat.clear();
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdir();
                setupDefaultFile("chat-motd.txt");
            }
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(setupDefaultFile("config.yml"));
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private File setupDefaultFile(String name) throws IOException {
        File file = new File(getDataFolder(), name);
        if (!file.exists()) {
            try (InputStream is = getResourceAsStream(name);
                 OutputStream os = new FileOutputStream(file)) {
                ByteStreams.copy(is, os);
            }
        }
        return file;
    }

    public Configuration getConfig() { return config; }

}
