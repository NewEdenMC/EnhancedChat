package co.neweden.enhancedchat;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

public class EnhancedChat {

    protected static Main plugin;

    public static Main getPlugin() { return plugin; }

    public static Logger getLogger() { return plugin.getLogger(); }

    public static Path evalPath(String path) throws InvalidPathException {
        if (path.equals(""))
            throw new InvalidPathException(path, "Path cannot be empty.");

        if (path.contains(".."))
            throw new InvalidPathException(path, "When trying to load the file path '" + path + "' contained '..', for security paths containing two dots are not allowed as they could be used to load files outside of the plugin folder.");

        return Paths.get(getPlugin().getDataFolder().getPath() + File.separator + path);
    }

    public static BaseComponent[] safeGetFormattedFile(String filePath) throws InvalidPathException, IOException {
        List<String> lines = Files.readAllLines(evalPath(filePath), StandardCharsets.UTF_8);
        StringBuffer sb = new StringBuffer();
        for (String line : lines) {
            sb.append(line + '\n');
        }
        String string = sb.toString().substring(0, sb.length() - 2);
        String formatted = ChatColor.translateAlternateColorCodes('&', string);
        return TextComponent.fromLegacyText(formatted);
    }

}
