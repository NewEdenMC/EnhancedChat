package co.neweden.enhancedchat;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EnhancedChat {

    protected static Main plugin;
    protected static Map<String, TextComponent> formattedChat = new HashMap<>();

    public static Main getPlugin() { return plugin; }

    public static Logger getLogger() { return plugin.getLogger(); }

    public static Path evalPath(String path) throws InvalidPathException {
        if (path.equals(""))
            throw new InvalidPathException(path, "Path cannot be empty.");

        if (path.contains(".."))
            throw new InvalidPathException(path, "When trying to load the file path '" + path + "' contained '..', for security paths containing two dots are not allowed as they could be used to load files outside of the plugin folder.");

        return Paths.get(getPlugin().getDataFolder().getPath() + File.separator + path);
    }

    public static TextComponent evalMessage(String message) { return evalMessage(message, null); }
    public static TextComponent evalMessage(String message, Map<String, String> extraTokens) {
        Map<String, String> tokens = new HashMap<>();
        tokens.put("%onlinePlayerCount%", String.valueOf(ProxyServer.getInstance().getOnlineCount()));
        if (extraTokens != null)
            tokens.putAll(extraTokens);

        message = new StringEval(message, tokens).toString();

        return new TextComponent(TextComponent.fromLegacyText(message));
    }

    public static TextComponent safeGetFormattedFile(String filePath) throws InvalidPathException, IOException {
        if (formattedChat.containsKey(filePath))
            return formattedChat.get(filePath);

        List<String> lines = Files.readAllLines(evalPath(filePath), StandardCharsets.UTF_8);
        StringBuffer sb = new StringBuffer();
        for (String line : lines) {
            sb.append(line + '\n');
        }

        String string = sb.toString().substring(0, sb.length() - 1);
        TextComponent tc = evalMessage(string);
        formattedChat.put(filePath, tc);

        return tc;
    }

    public static boolean sendMessageFromPath(CommandSender sender, String path, String errorMessage) {
        if (path.isEmpty()) return false;
        try {
            sender.sendMessage(EnhancedChat.safeGetFormattedFile(path));
            return true;
        } catch (Exception e) {
            EnhancedChat.getLogger().log(Level.WARNING, e.getMessage(), e);
            sender.sendMessage(new ComponentBuilder(errorMessage).color(ChatColor.RED).create());
            return false;
        }
    }

}
