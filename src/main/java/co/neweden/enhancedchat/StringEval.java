package co.neweden.enhancedchat;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.HashMap;
import java.util.Map;

/**
 * Takes strings and performs evaluations on them, a new instance of this class should be created for each String to be
 * evaluated.  Access this class through the static methods in the EnhancedChat class.
 */
public class StringEval {

    private String stringToEval;
    private TextComponent textComponent;
    private StringBuilder segment = new StringBuilder();
    private StringBuilder token = new StringBuilder();
    private char[] chars;
    private Map<String, String> tokens = new HashMap<>();
    private boolean inToken = false;
    private enum Action { CONTINUE, NEXT }
    private ChatColor colourCode;
    private int segmentWordStart = 0;
    private boolean inURL = false;

    private boolean stripFormatting = false;
    private boolean noEscape = false;

    private boolean evalURLs = true;
    private boolean underlineURLs = true;

    public StringEval(String string) { stringToEval = string; }

    public StringEval addTokens(Map<String, String> tokens) { this.tokens.putAll(tokens); return this;}

    public StringEval stripFormatting() { stripFormatting = true; return this; }

    public StringEval noEscapeBackslash() { noEscape = true; return this; }

    public StringEval urlEval(boolean enabled, boolean underlined) { evalURLs = enabled; underlineURLs = underlined; return this; }

    private void eval() {
        textComponent = new TextComponent();
        chars = stringToEval.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            // Double black-slash escape
            if (!noEscape && chars[i] == '\\' && i + 1 < chars.length && "%&\\".indexOf(chars[i + 1]) >= 0) {
                segment.append(chars[i + 1]);
                i++; continue;
            }

            // Evaluate for tokens
            if (evalToken(i) == Action.CONTINUE)
                continue;

            // Evaluate for colour codes
            if (stripFormatting) {
                if (chars[i] == '\u00A7')
                    continue;
            } else {
                if (chars[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(chars[i + 1]) >= 0) {
                    i++;
                    nextSegment();
                    colourCode = ChatColor.getByChar(chars[i]);
                    continue;
                }
            }

            // Evaluate for URLs
            if (evalURL(i) == Action.CONTINUE)
                continue;

            segment.append(chars[i]);
        }
        nextSegment();

    }

    private void nextSegment() {
        TextComponent tc = new TextComponent(segment.toString());
        tc.setColor(colourCode);
        if (inURL) {
            String rawURL = segment.toString();
            if (!rawURL.substring(0, 6).equalsIgnoreCase("http://") || !rawURL.substring(0, 7).equalsIgnoreCase("https://"))
                rawURL = "http://" + rawURL;
            tc.setUnderlined(underlineURLs);
            tc.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, rawURL));
            tc.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder("Click here to go to ").color(ChatColor.AQUA).append(rawURL).color(ChatColor.YELLOW).create()
            ));
            inURL = false;
        }
        textComponent.addExtra(tc);
        segment.setLength(0);
        segmentWordStart = 0;
    }

    /**
     * Evaluates the current character in the context of tokens
     *
     * @param i the current index
     * @return which action to perform, CONTINUE cause the char loop to move to the next char, NEXT cause the char loop
     * to move to the next evaluation
     */
    private Action evalToken(int i) {
        if (tokens.isEmpty()) return Action.NEXT;

        if (chars[i] == '%' && !inToken) {
            // token start detected, save position and char, continue to next char
            inToken = true; token.append(chars[i]); return Action.CONTINUE;
        }

        if (inToken && chars[i] != '%') {
            // currently in a token, save char for evaluation later, continue to next char
            token.append(chars[i]); return Action.CONTINUE;
        }

        if (inToken && chars[i] == '%') {
            // token end detected, if token exists append value otherwise append raw token, reset, continue to next char
            String ct = token.toString() + '%';
            segment.append(tokens.getOrDefault(ct, ct));
            inToken = false; token.setLength(0);
            return Action.CONTINUE;
        }

        // we are not in a token, so move to the next evaluation
        return Action.NEXT;
    }

    private Action evalURL(int i) {
        if (!evalURLs) return Action.NEXT;

        String spaceChars = " \t\n\r";

        if (inURL) {
            if (spaceChars.indexOf(chars[i]) >= 0) {
                nextSegment();
                return Action.NEXT;
            }
            segment.append(chars[i]);
            return Action.CONTINUE;
        }

        if (segment.length() < 1 || i + 1 > chars.length - 1)
            return Action.NEXT; // prevents ArrayIndexOutOfBounds Exceptions

        if (spaceChars.indexOf(chars[i - 1]) >= 0) {
            segmentWordStart = segment.length(); // will be the index of the last space
            return Action.NEXT;
        } // we need to keep track of where the last space char was

        if (spaceChars.indexOf(chars[i - 1]) >= 0 || chars[i] != '.' || spaceChars.indexOf(chars[i + 1]) >= 0)
            return Action.NEXT; // we are not in a URL as the chars around position i are space chars and char at i is not a dot

        char[] firstPart = new char[segment.length() - segmentWordStart];
        segment.getChars(segmentWordStart, segment.length(), firstPart, 0);
        segment.setLength(segment.length() - (segment.length() - segmentWordStart));
        nextSegment();
        segment.append(firstPart).append('.');
        inURL = true;

        return Action.CONTINUE;
    }

    public TextComponent getTextComponent() {
        if (textComponent == null) eval();
        return textComponent;
    }

    @Override
    public String toString() { return getTextComponent().toString(); }

}
