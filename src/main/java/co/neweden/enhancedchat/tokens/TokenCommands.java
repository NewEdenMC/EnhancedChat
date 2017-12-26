package co.neweden.enhancedchat.tokens;

import co.neweden.enhancedchat.EnhancedChat;
import co.neweden.enhancedchat.playerdata.Group;
import co.neweden.enhancedchat.playerdata.PlayerData;
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
import java.util.UUID;

public class TokenCommands extends Command {

    private Token token;
    private enum Type { PLAYER, GROUP }

    TokenCommands(Token token, List<String> aliases) {
        super(token.getName(), "enhancedchat.token." + token.getName(), aliases.toArray(new String[0]));
        this.token = token;
        ProxyServer.getInstance().getPluginManager().registerCommand(EnhancedChat.getPlugin(), this);
    }

    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new ComponentBuilder("Only Players can run this command.").color(ChatColor.RED).create());
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;
        runCommand(Type.PLAYER, token, sender, player.getUniqueId(), player.getDisplayName(), null, "/" + getName(), args);
    }

    public static void adminExecute(CommandSender sender, String[] args) {
        String help = "To get a token value: token [TOKEN-NAME] player/group [NAME]\nTo set a token value: token [TOKEN-NAME] player/group [NAME] [NEW-VALUE]";

        if (args.length < 4) {
            sender.sendMessage(new ComponentBuilder("Not enough arguments:\n" + help).color(ChatColor.GRAY).italic(true).create());
            return;
        }

        Type type;
        try {
            type = Type.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(new ComponentBuilder("Third argument must be either \"player\" or \"group\":\n" + help).color(ChatColor.GRAY).italic(true).create());
            return;
        }

        Token token = CustomTokens.getToken(args[1]);
        if (token == null) {
            sender.sendMessage(new ComponentBuilder("The token \"" + args[1] + "\" is not registered as a Custom Token.").color(ChatColor.RED).create());
            return;
        }

        String[] newValue = Arrays.copyOfRange(args, 4, args.length);
        String targetName = args[3];
        UUID targetUUID = null;
        Group targetGroup = null;

        if (type == Type.PLAYER) {
            if (!token.isPlayersEnabled()) {
                sender.sendMessage(new ComponentBuilder("This token does not support values for Players.").color(ChatColor.RED).create()); return;
            }

            targetUUID = PlayerData.getUUIDFromName(targetName);
            if (targetUUID == null) {
                sender.sendMessage(new ComponentBuilder("The player '" + targetName + "' is not online or hasn't connected before, remember their name is case-sensitive").color(ChatColor.RED).create());
                return;
            }
        } else if (type == Type.GROUP) {
            if (!token.isGroupsEnabled()) {
                sender.sendMessage(new ComponentBuilder("This token does not support values for Groups.").color(ChatColor.RED).create()); return;
            }

            targetGroup = PlayerData.getGroup(targetName);
            if (targetGroup == null) {
                sender.sendMessage(new ComponentBuilder("The group name '" + targetName + "' does not exist, to get a list of groups run: enhancedchat groups").color(ChatColor.RED).create());
                return;
            }
        } else return;

        String helperCommand = "enhancedchat token " + token.getName() + " " + args[2] + " " + args[3];
        if (sender instanceof ProxiedPlayer)
            helperCommand = "/" + helperCommand;

        runCommand(type, token, sender, targetUUID, targetName, targetGroup, helperCommand, newValue);
    }

    private static void runCommand(Type type, Token token, CommandSender sender, UUID targetUUID, String targetName, Group targetGroup, String helperCommand, String... value) {
        boolean senderIsSelf = false;
        String subjectText = (type == Type.PLAYER ? "player '" : "group '") + targetName + "'";
        StringEval currentValue;

        if (type == Type.PLAYER) {
            senderIsSelf = sender instanceof ProxiedPlayer && ((ProxiedPlayer) sender).getUniqueId().equals(targetUUID);
            currentValue = token.getValue(targetUUID);
        } else
            currentValue = token.getValueForGroup(targetGroup);

        String removeHelpSelf =  "To remove your " + token.getLabel() + " use " + helperCommand + " off";
        String removeHelpOther = "To remove " + token.getLabel() + " for " + subjectText + " use " + helperCommand + " off";
        ComponentBuilder removeHelp = new ComponentBuilder(senderIsSelf ? removeHelpSelf : removeHelpOther).color(ChatColor.GRAY).italic(true);

        if (value.length <= 0) {
            noArgs(sender, token, currentValue, subjectText, senderIsSelf, removeHelp); return;
        }

        String newValue = buildValue(value);
        String action;
        boolean success;
        String setValue;

        if (value[0].equalsIgnoreCase("off") || value[0].equalsIgnoreCase("remove")) {
            setValue = null;
            action = "remove";
        } else {
            setValue = newValue;
            action = "set";
        }

        if (type == Type.PLAYER)
            success = token.setPlayerValue(targetUUID, setValue);
        else
            success = token.setGroupValue(targetGroup, setValue);

        String setFailedSelf  = "Failed to " + action + " your " + token.getLabel() + ", please contact a member of staff.";
        String setFailedOther = "Failed to " + action + " the " + token.getLabel() + " for " + subjectText + ", please contact a member of staff.";

        if (!success) {
            sender.sendMessage(new ComponentBuilder(senderIsSelf ? setFailedSelf : setFailedOther).color(ChatColor.RED).create());
            return;
        }

        String removeSelf  = "Your " + token.getLabel() + " has been removed.";
        String removeOther = "The " + token.getLabel() + " for " + subjectText + " has been removed.";

        if (action.equals("remove")) {
            sender.sendMessage(new ComponentBuilder(senderIsSelf ? removeSelf : removeOther).color(ChatColor.BLUE).create());
            return;
        }

        TextComponent tcValue = new TextComponent(new StringEval(newValue).getTextComponent());
        tcValue.setColor(ChatColor.YELLOW);

        TextComponent message = new TextComponent(senderIsSelf ? "Your " + token.getLabel() : "The " + token.getLabel() + " for " + subjectText);
        if (currentValue != null) {
            TextComponent old = new TextComponent(currentValue.getTextComponent());
            old.setColor(ChatColor.YELLOW);
            message.addExtra(" has been changed from ");
            message.addExtra(old);
            message.addExtra(" to ");
        } else {
            message.addExtra(" has been set to ");
        }
        message.addExtra(tcValue);
        message.setColor(ChatColor.AQUA);
        sender.sendMessage(message);
        sender.sendMessage(removeHelp.create());
    }

    private static void noArgs(CommandSender sender, Token token, StringEval currentValue, String subjectText, boolean senderIsSelf, ComponentBuilder removeHelp) {
        String state;

        String noValSelf  = "You do not have a " + token.getLabel() + " set";
        String noValOther = "There is not currently a " + token.getLabel() + " set for " + subjectText;

        String currentValSelf  = "Your current " + token.getLabel() + " is ";
        String currentValOther = "The current " + token.getLabel() + " for " + subjectText + " is ";

        if (currentValue == null) {
            sender.sendMessage(new ComponentBuilder(senderIsSelf ? noValSelf : noValOther).color(ChatColor.AQUA).create());
            state = "set";
        } else {
            TextComponent current = new TextComponent(currentValue.getTextComponent());
            current.setColor(ChatColor.YELLOW);
            TextComponent message = new TextComponent(senderIsSelf ? currentValSelf : currentValOther);
            message.setColor(ChatColor.AQUA);
            message.addExtra(current);
            sender.sendMessage(message);
            state = "change";
        }

        String setSelf  = "To " + state + " your " + token.getLabel() + " run this command again followed by the " + token.getLabel() + " you want to use";
        String setOther = "To " + state + " the " + token.getLabel() + " for " + subjectText + " run this command again followed by the " + token.getLabel() + " you want to use";

        sender.sendMessage(new ComponentBuilder(senderIsSelf ? setSelf : setOther).color(ChatColor.AQUA).create());
        if (state.equals("change"))
            sender.sendMessage(removeHelp.create());
    }

    private static String buildValue(String[] args) {
        StringBuilder valueSB = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            valueSB.append(args[i]);
            if (args.length - 1 != i) valueSB.append(' ');
        }
        return valueSB.toString();
    }

}
