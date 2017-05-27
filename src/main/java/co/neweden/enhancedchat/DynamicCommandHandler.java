package co.neweden.enhancedchat;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

public class DynamicCommandHandler extends Command {

    private String fileName;

    public DynamicCommandHandler(String command, String fileName, String[] aliases) {
        super(command, null, aliases);
        this.fileName = fileName;
        ProxyServer.getInstance().getPluginManager().registerCommand(EnhancedChat.getPlugin(), this);
    }

    public void execute(CommandSender sender, String[] strings) {
        if (fileName.isEmpty()) {
            sender.sendMessage(new ComponentBuilder("Unable to get content for command, file name is missing or empty, please contact a member of staff.").color(ChatColor.RED).create());
            return;
        }

        EnhancedChat.sendMessageFromPath(sender, fileName, "An error occurred while trying to load the file for this command, please contact a member of staff.");
    }

}
