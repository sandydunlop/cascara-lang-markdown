package io.github.qishr.cascara.lang.markdown.processor;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.lang.LanguageOptions;
import io.github.qishr.cascara.common.lang.processor.Processor;
import io.github.qishr.cascara.common.lang.processor.Tokenizer;
import io.github.qishr.cascara.lang.markdown.MarkdownOptions;
import io.github.qishr.cascara.lang.markdown.token.MarkdownToken;
import io.github.qishr.cascara.lang.markdown.token.MarkdownTokenType;

public class MarkdownTokenizer implements Tokenizer<MarkdownToken> {
    private Reporter reporter;
    private MarkdownOptions options;
    private int pos = 0;
    private char c = 0;
    private int line = 1;
    private int col = 1;
    private List<MarkdownToken> tokens = new ArrayList<>();

    @Override
    public List<MarkdownToken> tokenize(String source) {
        return tokenize(source, null);
    }

    @Override
    public List<MarkdownToken> tokenize(String source, URI uri) {
        int length = source.length();
        tokens = new ArrayList<>();
        line = 1;
        col = 1;
        pos = 0;

        while (pos < length) {
            c = source.charAt(pos);
            int startPos = pos;
            int startLine = line;
            int startCol = col;
            boolean isAtLineStart = (col == 1);

            // 1. Handle Whitespace (Indents vs generic Spacing)
            if (Character.isWhitespace(c) && c != '\n') {
                StringBuilder expandedLexeme = new StringBuilder();
                while (pos < length && Character.isWhitespace(source.charAt(pos)) && source.charAt(pos) != '\n') {
                    char current = source.charAt(pos);
                    if (current == '\t') {
                        int tabWidth = (options != null) ? options.getIndentSize() : 4;
                        int spacesToAdd = tabWidth - ((col - 1) % tabWidth);
                        expandedLexeme.append(" ".repeat(spacesToAdd));
                        col += spacesToAdd;
                    } else {
                        expandedLexeme.append(current);
                        col++;
                    }
                    pos++;
                }
                MarkdownTokenType type = isAtLineStart ? MarkdownTokenType.INDENT : MarkdownTokenType.WHITESPACE;
                addToken(new MarkdownToken(type, expandedLexeme.toString(), null, startPos, startLine, startCol));
                continue;
            }

            // 2. Handle Newlines
            if (c == '\n') {
                addToken(new MarkdownToken(MarkdownTokenType.NEWLINE, "\n", null, pos, line, col));
                pos++;
                line++;
                col = 1;
                continue;
            }

            // 3. Handle Escapes and Backslashes
            if (c == '\\') {
                // Check if there's a character following the backslash
                if (pos + 1 < source.length()) {
                    char next = source.charAt(pos + 1);

                    // If it's punctuation, we treat it as an escaped character sequence (e.g., "\*")
                    if ("!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~".indexOf(next) != -1) {
                        String lexeme = source.substring(pos, pos + 2);
                        // Uses the startPos/startLine/startCol declared at the top of the loop
                        addToken(new MarkdownToken(MarkdownTokenType.ESCAPED_CHAR, lexeme, String.valueOf(next), startPos, startLine, startCol));
                        pos += 2;
                        col += 2;
                        continue;
                    }
                }

                // Otherwise, it's just a literal backslash (often a hard line break marker)
                addToken(new MarkdownToken(MarkdownTokenType.ESCAPED_CHAR, "\\", "\\", startPos, startLine, startCol));
                pos++;
                col++;
                continue;
            }

            if (Character.isDigit(c)) {
                int lookahead = pos;
                while (lookahead < length && Character.isDigit(source.charAt(lookahead))) {
                    lookahead++;
                }

                // Check if the digit sequence is followed by a dot
                if (lookahead < length && source.charAt(lookahead) == '.') {
                    String lexeme = source.substring(pos, lookahead + 1);
                    addToken(new MarkdownToken(MarkdownTokenType.LIST_NUMBER, lexeme, null, pos, line, col));

                    int consumed = lexeme.length();
                    pos += consumed;
                    col += consumed;
                    continue;
                }
            }

            // 4. Handle Known Symbols (Mid-word or Block starts)
            MarkdownTokenType type = matchSymbol(c);
            if (type != null) {
                addToken(new MarkdownToken(type, String.valueOf(c), null, pos, line, col));
                pos++;
                col++;
            } else {
                // 5.
                StringBuilder sb = new StringBuilder();
                while (pos < length) {
                    char current = source.charAt(pos);

                    // Stop if we hit a symbol, whitespace, or escape
                    if (isSymbol(current) || Character.isWhitespace(current) || current == '\\') {
                        break;
                    }

                    // Special case: If it's a digit, only stop if it's the start of a "1."
                    if (Character.isDigit(current)) {
                        if (isStartOfOrderedList(source, pos)) {
                            break;
                        }
                    }

                    sb.append(current);
                    pos++;
                    col++;
                }

                if (sb.length() > 0) {
                    addToken(new MarkdownToken(MarkdownTokenType.TEXT, sb.toString(), sb.toString(), startPos, startLine, startCol));
                } else {
                    // SAFETY VALVE: If we reach here and haven't moved,
                    // it means isSymbol was true but matchSymbol was null.
                    // We'll treat the unknown char as a single-char TEXT token to move forward.
                    String unknown = String.valueOf(source.charAt(pos));
                    addToken(new MarkdownToken(MarkdownTokenType.TEXT, unknown, unknown, pos, line, col));
                    pos++;
                    col++;
                }
            }
        }
        return tokens;
    }

    private void addToken(MarkdownToken token) {
        trace("addToken");
        tokens.add(token);
    }

    private boolean isSymbol(char c) {
        return matchSymbol(c) != null;
    }

    private boolean isStartOfOrderedList(String source, int p) {
        int look = p;
        while (look < source.length() && Character.isDigit(source.charAt(look))) look++;
        return look < source.length() && source.charAt(look) == '.';
    }

    private MarkdownTokenType matchSymbol(char c) {
        return switch (c) {
            case '|' -> MarkdownTokenType.PIPE;
            case '=' -> MarkdownTokenType.EQUALS;
            case '#' -> MarkdownTokenType.HASH;
            case '*' -> MarkdownTokenType.ASTERISK;
            case '-' -> MarkdownTokenType.DASH;
            case '_' -> MarkdownTokenType.UNDERSCORE;
            case '<' -> MarkdownTokenType.LT;
            case '>' -> MarkdownTokenType.GT;
            case '[' -> MarkdownTokenType.BRACKET_OPEN;
            case ']' -> MarkdownTokenType.BRACKET_CLOSE;
            case '(' -> MarkdownTokenType.PAREN_OPEN;
            case ')' -> MarkdownTokenType.PAREN_CLOSE;
            case '!' -> MarkdownTokenType.BANG;
            case '`' -> MarkdownTokenType.BACKTICK;
            case ':' -> MarkdownTokenType.COLON;
            case '"' -> MarkdownTokenType.QUOTE;
            case '\''-> MarkdownTokenType.SINGLE_QUOTE;
            default  -> null;
        };
    }

    @Override public MarkdownTokenizer setReporter(Reporter reporter) { this.reporter = reporter; return this; }
    @Override public MarkdownTokenizer setOptions(LanguageOptions<?> options) { this.options = (MarkdownOptions)options; return this; }

    //
    // Diagnostics
    //

    private void trace(String method) {
        if (reporter == null) return;
        reporter.trace("P=%03d '%s' %03d:%03d %s", pos, currentChar(c), line, col, method);
    }

    private String currentChar(char c) {
        switch (c) {
            case ' ':
                return "␠";
            case '\t':
                return "⇥";
            case '\r':
                return "↵";
            case '\n':
                return "↩";
            default:
                return Character.toString(c);
        }
    }
}
