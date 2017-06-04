package co.neweden.enhancedchat;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.Map;

/**
 * Takes strings and performs evaluations on them, a new instance of this class should be created for each String to be
 * evaluated.  Access this class through the static methods in the EnhancedChat class.
 */
public class StringEval {

    private TextComponent textComponent = new TextComponent();
    private StringBuilder segment = new StringBuilder();
    private StringBuilder token = new StringBuilder();
    private char[] chars;
    private Map<String, String> tokens;
    private int tokenStart = 0;
    private enum Action { CONTINUE, NEXT }
    private ChatColor colourCode;
    private int segmentWordStart = 0;
    private boolean inURL = false;

    protected StringEval() { }

    protected StringEval(String string, Map<String, String> tokens) {
        chars = string.toCharArray();
        this.tokens = tokens;

        for (int i = 0; i < chars.length; i++) {
            // Double black-slash escape
            if (chars[i] == '\\' && i + 1 < chars.length && "%&\\".indexOf(chars[i + 1]) >= 0) {
                segment.append(chars[i + 1]);
                i++; continue;
            }

            // Evaluate for tokens
            if (evalToken(i) == Action.CONTINUE)
                continue;

            // Evaluate for colour codes
            if (chars[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(chars[i + 1]) >= 0) {
                i++; nextSegment();
                colourCode = ChatColor.getByChar(chars[i]);
                continue;
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
            tc.setUnderlined(true);
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
        if (chars[i] == '%' && tokenStart == 0 && !shouldEscape(i)) {
            // token start detected, save position and char, continue to next char
            tokenStart = i; token.append(chars[i]); return Action.CONTINUE;
        }

        if (tokenStart != 0 && chars[i] != '%') {
            // currently in a token, save char for evaluation later, continue to next char
            token.append(chars[i]); return Action.CONTINUE;
        }

        if (tokenStart != 0 && chars[i] == '%') {
            // token end detected, if token exists append value otherwise append raw token, reset, continue to next char
            String ct = token.toString() + '%';
            segment.append(tokens.getOrDefault(ct, ct));
            tokenStart = 0; token.setLength(0);
            return Action.CONTINUE;
        }

        // we are not in a token, so move to the next evaluation
        return Action.NEXT;
    }

    private Action evalURL(int i) {
        String spaceChars = " \t\n\r";

        if (inURL) {
            if (spaceChars.indexOf(chars[i]) >= 0) {
                nextSegment();
                return Action.NEXT;
            }
            segment.append(chars[i]);
            return Action.CONTINUE;
        }

        if (spaceChars.indexOf(chars[i - 1]) >= 0) {
            segmentWordStart = segment.length(); // will be the index of the last space
            return Action.NEXT;
        } // we need to keep track of where the last space char was

        if (segment.length() < 1 || i + 1 > chars.length - 1)
            return Action.NEXT; // prevents ArrayIndexOutOfBounds Exceptions

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

    private boolean shouldEscape(int i) {
        return !(chars[i - 2] == '\\' && chars[i - 1] == '\\') || chars[i - 1] == '\\';
    }

    public TextComponent getTextComponent() { return textComponent; }

    @Override
    public String toString() {
        return textComponent.toString();
    }

}
