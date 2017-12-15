package co.neweden.enhancedchat;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class Commands extends Command {

    protected Commands() {
        super("enhancedchat", "enhancedchat.admin");
        ProxyServer.getInstance().getPluginManager().registerCommand(EnhancedChat.getPlugin(), this);
    }

    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            help(sender); return;
        }

        switch (args[0].toLowerCase()) {
            case "reload" : reload(sender); return;
        }
    }

    private static void help(CommandSender sender) {
        sender.sendMessage(new ComponentBuilder("Enhanced Chat Admin Sub-Commands:").color(ChatColor.WHITE).create());

        String[] cmd = new String[1];
        String[] desc = new String[1];

        cmd[0] = "reload"; desc[0] = "Reload the configuration and clear the cache";

        for (int i = 0; i < cmd.length; i++) {
            sender.sendMessage(
                    new ComponentBuilder("- ").color(ChatColor.WHITE)
                            .append(cmd[i]).color(ChatColor.AQUA)
                            .append(": " + desc[i]).color(ChatColor.WHITE).create()
            );
        }
    }

    private static void reload(CommandSender sender) {
        EnhancedChat.getPlugin().reload();
        sender.sendMessage(new ComponentBuilder("Config reloaded and cache cleared.").color(ChatColor.GREEN).create());
    }

}
