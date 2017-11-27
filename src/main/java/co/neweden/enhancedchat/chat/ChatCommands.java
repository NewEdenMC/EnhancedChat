package co.neweden.enhancedchat.chat;

import co.neweden.enhancedchat.EnhancedChat;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class ChatCommands extends Command {

    protected ChatCommands() {
        super("ch", null, "chat", "channel");
        ProxyServer.getInstance().getPluginManager().registerCommand(EnhancedChat.getPlugin(), this);
    }

    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
                    "&fChannel Sub-Commands:\n" +
                    "&f- &blist&f: Show a list of channels available to you\n" +
                    "&f- &bjoin {CHANNEL-NAME]&f: Join the specified channel\n" +
                    "&f- &bleave {CHANNEL-NAME]&f: Leave the specified channel\n" +
                    "&f- &b{CHANNEL-NAME]&f: Change the channel you are currently speaking in to another channel"
            ))); return;
        }

        switch (args[0].toLowerCase()) {
            case "list" : list(sender); break;
            case "join" : join(sender, args); break;
            case "leave" : leave(sender, args); break;
            default: change(sender, args); break;
        }
    }

    private void list(CommandSender sender) {
        ProxiedPlayer player = null;
        if (sender instanceof ProxiedPlayer)
            player = (ProxiedPlayer) sender;

        StringBuilder out = new StringBuilder(player != null ? "&fAvailable Channels:" : "&fChannel List:");

        int size = 0;
        for (Channel channel : ChatManager.getChannels()) {
            boolean joined = false;
            boolean active = false;
            if (player != null) {
                joined = channel.getChatters().contains(player);
                if (!channel.canJoin(player) && !joined) continue;
                active = channel.equals(ChatManager.getActiveChannel(player));
            }

            out.append('\n')
               .append("&f[").append(channel.getShortName()).append("] ")
               .append(channel.getName());

            if (player != null)
                out.append(joined ? " &a+" : " &c-")
                   .append(active ? " &e*" : "");

            size++;
        }

        if (player != null) {
            out.append('\n').append('\n');
            out.append("&7&o'&a&o+&7&o' after a channel means you have joined that channel\n");
            out.append("&7&o'&c&o-&7&o' after a channel manes you have not joined that channel\n");
            out.append("&7&o'&e&o*&7&o' after a channel means this is the channel you are talking in");
        }

        if (size == 0)
            out = new StringBuilder("&7&oThere are no channels available :(");

        sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', out.toString())));
    }

    private void join(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new ComponentBuilder("Only Players can join channels.").color(ChatColor.RED).create()); return;
        }
        ProxiedPlayer player = (ProxiedPlayer) sender;

        if (args.length < 2) {
            player.sendMessage(new ComponentBuilder("You need to specify which channel you would like to join.").color(ChatColor.RED).create()); return;
        }

        Channel channel = ChatManager.getChannel(args[1]);
        if (channel == null || !channel.canJoin(player)) {
            player.sendMessage(new ComponentBuilder("That channel does not exist or you are not able to join it.").color(ChatColor.RED).create()); return;
        }

        if (channel.getChatters().contains(player)) {
            player.sendMessage(new ComponentBuilder("You have already joined this channel, to change the channel you are talking in use '/ch " + args[0] + "' or to send a quick message using '/ch " + args[0] + " [YOUR-MESSAGE]'").color(ChatColor.GREEN).create()); return;
        }

        channel.joinPlayer(player);
    }

    private void leave(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new ComponentBuilder("Only Players can leave channels.").color(ChatColor.RED).create()); return;
        }
        ProxiedPlayer player = (ProxiedPlayer) sender;

        if (args.length < 2) {
            player.sendMessage(new ComponentBuilder("You need to specify which channel you would like to leave.").color(ChatColor.RED).create()); return;
        }

        Channel channel = ChatManager.getChannel(args[1]);
        if (channel == null || !channel.getChatters().contains(player)) {
            player.sendMessage(new ComponentBuilder("You cannot leave this channel because you have not joined it, to see a list of available channels use '/ch list'").color(ChatColor.GREEN).create()); return;
        }

        channel.removePlayer(player);
    }

    private void change(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new ComponentBuilder("Only Players can change channels.").color(ChatColor.RED).create()); return;
        }
        ProxiedPlayer player = (ProxiedPlayer) sender;

        if (args.length < 1) {
            player.sendMessage(new ComponentBuilder("You need to specify which channel you would like to change to.").color(ChatColor.RED).create()); return;
        }

        Channel toChannel = ChatManager.getChannel(args[0]);
        if (toChannel == null) {
            player.sendMessage(new ComponentBuilder("That channel you are trying to change to does not exist or you are not able to join it.").color(ChatColor.RED).create()); return;
        }

        if (!toChannel.getChatters().contains(player)) {
            if (toChannel.canJoin(player))
                toChannel.joinPlayer(player);
            else {
                player.sendMessage(new ComponentBuilder("That channel you are trying to change to does not exist or you are not able to join it.").color(ChatColor.RED).create()); return;
            }
        }

        ChatManager.setActiveChannel(player, toChannel);
    }

}
