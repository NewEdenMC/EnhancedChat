package co.neweden.enhancedchat.chat.privatemessage;

import co.neweden.enhancedchat.EnhancedChat;
import co.neweden.enhancedchat.chat.ChatManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

import java.util.Arrays;

public class PrivateMessageCommand extends Command {

    PrivateMessageCommand(String command, String... aliases) {
        super(command, null, aliases);
        ProxyServer.getInstance().getPluginManager().registerCommand(EnhancedChat.getPlugin(), this);
    }

    public void execute(CommandSender sender, String[] args) {
        PrivateMessageManager pmm = ChatManager.getPrivateMessageManager();
        CommandSender to;
        String[] newArgs;

        if (!getName().equals("reply")) {
            if (args.length < 1) {
                sender.sendMessage(new ComponentBuilder("You must specify which Player you would like to send a private message to: /" + getName() + " <name> <message>").color(ChatColor.RED).create()); return;
            }
            to = ProxyServer.getInstance().getPlayer(args[0]);
            if (to == null) {
                sender.sendMessage(new ComponentBuilder("The Player that you specified '" + args[0] + "' is not online, did you mistype their name? Remember names are case-sensitive, try using TAB Complete.").color(ChatColor.RED).create()); return;
            }
            newArgs = Arrays.copyOfRange(args, 1, args.length);

        } else {

            to = pmm.getWhoSentLastMessage(sender);
            if (to == null) {
                sender.sendMessage(new ComponentBuilder("You cannot send a reply as there is no one to reply to, once someone sends you a message, you can use this command to reply directly to them.").color(ChatColor.RED).create()); return;
            }
            newArgs = args;
        }

        if (newArgs.length < 1) {
            sender.sendMessage(new ComponentBuilder("You did not enter a private message to send.").color(ChatColor.RED).create()); return;
        }

        StringBuilder message = new StringBuilder();
        for (int i = 0; i < newArgs.length; i++) {
            message.append(args[i]);
            if (i <= newArgs.length - 2)
                message.append(' ');
        }

        pmm.sendPrivateMessage(sender, to, message.toString());
    }

}
