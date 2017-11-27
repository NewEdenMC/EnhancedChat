package co.neweden.enhancedchat.chat.commands;

import co.neweden.enhancedchat.EnhancedChat;
import co.neweden.enhancedchat.chat.ChatManager;
import co.neweden.enhancedchat.chat.Message;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class EmoteCommand extends Command {

    public EmoteCommand() {
        super("emote", null, "me");
        ProxyServer.getInstance().getPluginManager().registerCommand(EnhancedChat.getPlugin(), this);
    }

    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new ComponentBuilder("Only Players can run channel commands.").color(ChatColor.RED).create()); return;
        }

        if (args.length < 1) {
            sender.sendMessage(new ComponentBuilder("Youu must provide a message: /" + getName() + " <message>").color(ChatColor.RED).create()); return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;

        StringBuilder emote = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            emote.append(args[i]);
            if (i <= args.length - 2)
                emote.append(' ');
        }

        ChatManager.sendMessageToActiveChannel(player, Message.Format.EMOTE, emote.toString());
    }

}
