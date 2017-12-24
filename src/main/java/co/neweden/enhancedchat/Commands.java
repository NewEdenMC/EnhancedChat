package co.neweden.enhancedchat;

import co.neweden.enhancedchat.playerdata.Group;
import co.neweden.enhancedchat.playerdata.PlayerData;
import co.neweden.enhancedchat.tokens.TokenCommands;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Collection;

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
            case "token" : TokenCommands.adminExecute(sender, args); return;
            case "groups" : groups(sender, args); return;
        }
    }

    private static void help(CommandSender sender) {
        sender.sendMessage(new ComponentBuilder("Enhanced Chat Admin Sub-Commands:").color(ChatColor.WHITE).create());

        String[] cmd = new String[6];
        String[] desc = new String[6];

        cmd[0] = "reload"; desc[0] = "Reload the configuration and clear the cache";
        cmd[1] = "token [TOKEN-NAME] player [PLAYER-NAME]"; desc[1] = "Get or set the Custom Token for the given Player";
        cmd[2] = "token [TOKEN-NAME] group [GROUP-NAME]"; desc[2] = "Get or set the Custom Token for the given Group";
        cmd[3] = "groups"; desc[3] = "List groups";
        cmd[4] = "groups player [PLAYER-NAME]"; desc[4] = "Show which groups a player is a member of";
        cmd[5] = "groups rebuildcache"; desc[5] = "Rebuilds the cached data that stores which groups players are members of";

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

    private static void groups(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            sender.sendMessage(new ComponentBuilder("Current groups and their rank:").create());
            for (Group group : PlayerData.getGroups()) {
                sender.sendMessage(new ComponentBuilder("- " + group.getName() + " (Rank: " + group.getRank() + ")").create());
            }
            return;
        }

        if (args[1].equalsIgnoreCase("player")) {
            BaseComponent[] error = new ComponentBuilder("You must specify the name of a currently online player").color(ChatColor.RED).create();
            if (args.length <= 2) { sender.sendMessage(error); return; }

            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(args[2]);
            if (player == null) { sender.sendMessage(error); return; }

            Collection<Group> groups = PlayerData.getGroupsForPlayer(player);
            if (groups.size() < 1) {
                sender.sendMessage(new ComponentBuilder("'" + player.getName() + "' is not in any groups :(").color(ChatColor.GRAY).italic(true).create());
                return;
            }
            sender.sendMessage(new ComponentBuilder("Group Membership for '" + player.getName() + "':").create());
            for (Group group : groups) {
                sender.sendMessage(new ComponentBuilder("- " + group.getName() + " (Rank: " + group.getRank() + ")").create());
            }
            return;
        }

        if (args[1].equalsIgnoreCase("rebuildcache")) {
            PlayerData.rebuildPlayerGroupCache();
            sender.sendMessage(new ComponentBuilder("Rebuilt Group Cache for Players").color(ChatColor.AQUA).create());
            return;
        }

        sender.sendMessage(new ComponentBuilder("Unknown sub-command").color(ChatColor.RED).create());
    }

}
