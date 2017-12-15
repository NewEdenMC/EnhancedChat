package co.neweden.enhancedchat.chat.commands;

import co.neweden.enhancedchat.EnhancedChat;
import co.neweden.enhancedchat.chat.Channel;
import co.neweden.enhancedchat.chat.ChatManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Collection;

public class ChatCommands extends Command {

    public ChatCommands() {
        super("ch", null, "chat", "channel");
        ProxyServer.getInstance().getPluginManager().registerCommand(EnhancedChat.getPlugin(), this);
    }

    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            help(sender); return;
        }

        switch (args[0].toLowerCase()) {
            case "list" : list(sender); return;
        }

        if (sender instanceof ProxiedPlayer) {
            switch (args[0].toLowerCase()) {
                case "join" : join(sender, args); return;
                case "leave" : leave(sender, args); return;
                default: change(sender, args); return;
            }
        }

        sender.sendMessage(new ComponentBuilder("Unknown sub-command").color(ChatColor.RED).create());
    }

    private static void help(CommandSender sender) {
        sender.sendMessage(new ComponentBuilder("Chat Sub-Commands:").color(ChatColor.WHITE).create());

        String[] cmd = new String[4];
        String[] desc = new String[4];
        Boolean[] checkPlayer = new Boolean[4];

        cmd[0] = "list"; desc[0] = "Show a list of channels available to you"; checkPlayer[0] = false;
        cmd[1] = "join [CHANNEL-NAME]"; desc[1] = "Join the specified channel"; checkPlayer[1] = true;
        cmd[2] = "leave [CHANNEL-NAME]"; desc[2] = "Leave the specified channel"; checkPlayer[2] = true;
        cmd[3] = "[CHANNEL-NAME]"; desc[3] = "Change the channel you are currently speaking in to another channel"; checkPlayer[3] = true;

        for (int i = 0; i < cmd.length; i++) {
            if (checkPlayer[i] && !(sender instanceof ProxiedPlayer)) continue;
            sender.sendMessage(
                    new ComponentBuilder("- ").color(ChatColor.WHITE)
                            .append(cmd[i]).color(ChatColor.AQUA)
                            .append(": " + desc[i]).color(ChatColor.WHITE).create()
            );
        }
    }

    private static void list(CommandSender sender) {
        Collection<Channel> channels = ChatManager.getChannels();

        if (channels.size() == 0) {
            sender.sendMessage(new ComponentBuilder("There are no channels available :(").color(ChatColor.GRAY).italic(true).create());
            return;
        }

        ProxiedPlayer player = null;
        if (sender instanceof ProxiedPlayer)
            player = (ProxiedPlayer) sender;

        ComponentBuilder out = new ComponentBuilder(player != null ? "Available Channels:" : "Channel List:").color(ChatColor.WHITE);

        for (Channel channel : ChatManager.getChannels()) {
            boolean joined = false;
            if (player != null) {
                joined = channel.getChatters().contains(player);
                if (!channel.canJoin(player) && !joined) continue;
            }

            out.append("\n[" + channel.getShortName() + "] " + channel.getName()).color(ChatColor.WHITE);

            if (player != null) {
                if (joined)
                    out.append(" +").color(ChatColor.GREEN);
                else
                    out.append(" -").color(ChatColor.RED);

                if (channel.equals(ChatManager.getActiveChannel(player)))
                    out.append(" *").color(ChatColor.YELLOW);
            }
        }

        sender.sendMessage(out.create());

        if (player == null) return;

        ChatColor[] symbolColor = new ChatColor[3];
        String[] symbol = new String[3];
        String[] desc = new String[3];

        symbolColor[0] = ChatColor.GREEN;  symbol[0] = "+"; desc[0] = "after a channel means you have joined that channel\n";
        symbolColor[1] = ChatColor.RED;    symbol[1] = "-"; desc[1] = "after a channel means you have not joined that channel\n";
        symbolColor[2] = ChatColor.YELLOW; symbol[2] = "*"; desc[2] = "after a channel means this is the channel you are talking in";

        TextComponent help = new TextComponent("\n"); help.setColor(ChatColor.GRAY); help.setItalic(true);
        for (int i = 0; i < 3; i++) {
            TextComponent tcsymbol = new TextComponent(symbol[i]); tcsymbol.setColor(symbolColor[i]);
            help.addExtra("'"); help.addExtra(tcsymbol); help.addExtra("'");
            help.addExtra(" " + desc[i]);
        }
        sender.sendMessage(help);
    }

    private static void join(CommandSender sender, String[] args) {
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

    private static void leave(CommandSender sender, String[] args) {
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

    private static void change(CommandSender sender, String[] args) {
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
