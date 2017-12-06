package co.neweden.enhancedchat.tokens;

import co.neweden.enhancedchat.EnhancedChat;
import co.neweden.enhancedchat.StringEval;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Arrays;
import java.util.List;

public class Commands extends Command {

    Token token;

    Commands(Token token, String name, List<String> aliases) {
        super(name, "enhancedchat.token." + name, aliases.toArray(new String[0]));
        this.token = token;
        ProxyServer.getInstance().getPluginManager().registerCommand(EnhancedChat.getPlugin(), this);
    }

    public void execute(CommandSender sender, String[] args) { userExecute(sender, args); }

    private void userExecute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new ComponentBuilder("Only Players can run this command.").color(ChatColor.RED).create());
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;
        StringEval currentValue = token.getValue(player);

        if (args.length <= 0) {
            String state;
            if (currentValue == null) {
                player.sendMessage(new ComponentBuilder("You do not currently have a " + token.getLabel() + " set.").color(ChatColor.AQUA).create());
                state = "set";
            } else {
                TextComponent current = new TextComponent(currentValue.getTextComponent());
                current.setColor(ChatColor.YELLOW);
                TextComponent message = new TextComponent("Your current " + token.getLabel() + " is: ");
                message.setColor(ChatColor.AQUA);
                message.addExtra(current);
                player.sendMessage(message);
                state = "change";
            }
            player.sendMessage(new ComponentBuilder("To " + state + " your " + token.getLabel() + " running this command again followed by the " + token.getLabel() + " you want to use").color(ChatColor.AQUA).create());
            if (state.equals("change"))
                player.sendMessage(new ComponentBuilder("To remove your " + token.getLabel() + " use: /" + getName() + " off").color(ChatColor.GRAY).italic(true).create());
            return;
        }

        String action;
        boolean success;
        if (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("remove")) {
            success = token.setPlayerValue(player, null);
            action = "remove";
        } else {
            success = buildSetToken(player, args);
            action = "set";
        }
        if (!success) {
            player.sendMessage(new ComponentBuilder("Failed to " + action + " your " + token.getLabel() + ", please contact a member of staff.").color(ChatColor.RED).create());
            return;
        }

        if (action.equals("remove")) {
            player.sendMessage(new ComponentBuilder("Your " + token.getLabel() + " has been removed.").color(ChatColor.BLUE).create());
            return;
        }

        TextComponent newValue = new TextComponent(token.getValue(player).getTextComponent());
        newValue.setColor(ChatColor.YELLOW);

        TextComponent message = new TextComponent("Your " + token.getLabel());
        if (currentValue != null) {
            TextComponent old = new TextComponent(currentValue.getTextComponent());
            old.setColor(ChatColor.YELLOW);
            message.addExtra(" has been changed from ");
            message.addExtra(old);
            message.addExtra(" to ");
        } else {
            message.addExtra(" has been set to ");
        }
        message.addExtra(newValue);
        message.setColor(ChatColor.AQUA);
        player.sendMessage(message);
        player.sendMessage(new ComponentBuilder("To remove your " + token.getLabel() + " use: /" + getName() + " off").color(ChatColor.GRAY).italic(true).create());
    }

    private boolean buildSetToken(ProxiedPlayer player, String[] args) {
        StringBuilder valueSB = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            valueSB.append(args[i]);
            if (args.length - 1 != i) valueSB.append(' ');
        }
        return token.setPlayerValue(player, valueSB.toString());
    }

    static void adminExecute(Token token, CommandSender sender, String[] args) {

    }

    public void old(CommandSender sender, String[] args) {
        String otherPermission = "enhancedchat.nick.others";
        /*if (args.length < 1) {
            if (sender.hasPermission(otherPermission)) {
                if (sender instanceof ProxiedPlayer) {
                    sender.sendMessage(new ComponentBuilder("You must provide a nickname or the username of another player followed by a nickname: /" + getName() + " [player] <nickname>").color(ChatColor.RED).create()); return;
                } else {
                    sender.sendMessage(new ComponentBuilder("You must provide the username of another player followed by a nickname: /" + getName() + " <player> <nickname>").color(ChatColor.RED).create()); return;
                }
            } else {
                sender.sendMessage(new ComponentBuilder("You must provide a nickname: /" + getName() + " <nickname>").color(ChatColor.RED).create()); return;
            }
        }

        ProxiedPlayer changing = null;
        String nickname = null;
        if (sender instanceof ProxiedPlayer && sender.hasPermission("enhancedchat.nick.other")) {
            changing = ProxyServer.getInstance().getPlayer(args[0]);
        }*/

        if (sender instanceof ProxiedPlayer) {
            if (args.length <= 0) {
                if (sender.hasPermission(otherPermission))
                    sender.sendMessage(new ComponentBuilder("You must provide a nickname or the username of another player followed by a nickname: /" + getName() + " [player] <nickname>").color(ChatColor.RED).create());
                else
                    sender.sendMessage(new ComponentBuilder("You must provide a nickname: /" + getName() + " <nickname>").color(ChatColor.RED).create());
                return;
            }

            if (sender.hasPermission(otherPermission)) {
                if (args.length == 1) {
                    changeNickname(sender, sender, args); return; // defiantly changing own nickname as only 1 arg
                }
                // we have more than 1 arg and sender can change nickname's of others so arg[0] should be other name
                ProxiedPlayer changing = ProxyServer.getInstance().getPlayer(args[0]);
                if (changing == null) {
                    sender.sendMessage(new ComponentBuilder("").create());
                }
            }

            if (args.length >= 1) {
                changeNickname(sender, sender, args); return; // defiantly changing own nickname as only 1 arg
            } else if (args.length < 1 && !sender.hasPermission(otherPermission)) {
                sender.sendMessage(new ComponentBuilder("You must provide a nickname: /" + getName() + " <nickname>").color(ChatColor.RED).create()); return;
            } else if (args.length >= 2 && sender.hasPermission(otherPermission)) {
                changeNickname(sender, sender, Arrays.copyOfRange(args, 1, args.length)); return; // two or more args and has permission to change other nicknames
            } else if (args.length < 2 && sender.hasPermission(otherPermission)) {
                sender.sendMessage(new ComponentBuilder("You must provide a nickname: /" + getName() + " <nickname>").color(ChatColor.RED).create()); return;
            }
        } else {
            // have to change other
        }

    }

    private void changeNickname(CommandSender sender, CommandSender beingChanged, String[] nickname) {

    }

}
