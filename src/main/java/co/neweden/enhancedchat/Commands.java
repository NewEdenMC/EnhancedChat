package co.neweden.enhancedchat;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class Commands extends Command {

    protected Commands() {
        super("enhancedchat", "enhancedchat.admin");
        ProxyServer.getInstance().getPluginManager().registerCommand(EnhancedChat.getPlugin(), this);
    }

    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
                    "Enhanced Chat Admin Commands:\n" +
                    "&f- &breload&f: Reload the configuration and clear the cache"
            )));
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            EnhancedChat.getPlugin().reload();
            sender.sendMessage(new ComponentBuilder("Config reloaded and cache cleared.").color(ChatColor.GREEN).create());
        }
    }

}
