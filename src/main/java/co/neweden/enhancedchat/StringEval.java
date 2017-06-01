package co.neweden.enhancedchat;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;

import java.util.Map;

/**
 * Takes strings and performs evaluations on them, a new instance of this class should be created for each String to be
 * evaluated.  Access this class through the static methods in the EnhancedChat class.
 */
public class StringEval {

    private ComponentBuilder componentBuilder = new ComponentBuilder("");
    private StringBuilder segment = new StringBuilder();
    private StringBuilder token = new StringBuilder();
    private char[] chars;
    private Map<String, String> tokens;
    private int tokenStart = 0;
    private enum Action { CONTINUE, NEXT }
    private ChatColor colourCode;

    protected StringEval() { }

    protected StringEval(String string, Map<String, String> tokens) {
        chars = string.toCharArray();
        this.tokens = tokens;

        for (int i = 0; i < chars.length; i++) {
            // Evaluate for tokens
            if (evalToken(i) == Action.CONTINUE)
                continue;

            // Evaluate for colour codes
            if (chars[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(chars[i + 1]) >= 0) {
                i++;
                nextSegment();
                colourCode = ChatColor.getByChar(chars[i]);
                continue;
            }

            segment.append(chars[i]);
        }
        nextSegment();

    }

    private void nextSegment() {
        componentBuilder.append(segment.toString()).color(colourCode);
        segment.setLength(0);
    }

    /**
     * Evaluates the current character in the context of tokens
     *
     * @param i the current index
     * @return which action to perform, CONTINUE cause the char loop to move to the next char, NEXT cause the char loop
     * to move to the next evaluation
     */
    private Action evalToken(int i) {
        if (chars[i] == '%' && tokenStart == 0) {
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

    public ComponentBuilder getComponentBuilder() { return componentBuilder; }

    @Override
    public String toString() {
        return componentBuilder.toString();
    }

}
