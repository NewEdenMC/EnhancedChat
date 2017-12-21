package co.neweden.enhancedchat.chat.commands;

import co.neweden.enhancedchat.EnhancedChat;
import co.neweden.enhancedchat.chat.Channel;
import co.neweden.enhancedchat.chat.Message;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class QuickMessageCommand extends Command {

    private Channel channel;

    public QuickMessageCommand(Channel channel) {
        super(channel.getShortName());
        this.channel = channel;
        ProxyServer.getInstance().getPluginManager().registerCommand(EnhancedChat.getPlugin(), this);
    }

    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof ProxiedPlayer && !channel.getChatters().contains(sender)) {
            sender.sendMessage(new ComponentBuilder("You are not in this channel so can't send a quick message, you can try joining this channel.").color(ChatColor.RED).create());
            return;
        }
        if (args.length <= 0) {
            sender.sendMessage(new ComponentBuilder("You need to provide a message to send to this channel.").color(ChatColor.RED).create());
            return;
        }

        StringBuilder message = new StringBuilder();
        for (String arg : args) {
            message.append(arg).append(' ');
        }
        String out = message.substring(0, message.length() - 1);
        channel.sendMessage(sender, Message.Source.PLAYER, Message.Format.NORMAL, out);
    }

}
