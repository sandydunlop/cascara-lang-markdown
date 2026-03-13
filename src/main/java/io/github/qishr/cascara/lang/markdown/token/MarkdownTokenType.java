package io.github.qishr.cascara.lang.markdown.token;

import io.github.qishr.cascara.common.lang.token.TokenCategory;
import io.github.qishr.cascara.common.lang.token.TokenType;

public enum MarkdownTokenType implements TokenType {
    // Structural
    HASH("HASH", TokenCategory.OPERATOR),          // #
    INDENT("INDENT", TokenCategory.WHITESPACE),    // Leading spaces/tabs
    WHITESPACE("WS", TokenCategory.WHITESPACE),    // spaces/tabs
    NEWLINE("NL", TokenCategory.WHITESPACE),      // \n

    // Block Delimiters
    COLON("COLON", TokenCategory.OPERATOR),              // >
    QUOTE("QUOTE", TokenCategory.OPERATOR),              // >
    SINGLE_QUOTE("SINGLE_QUOTE", TokenCategory.OPERATOR),              // >
    LT("LT", TokenCategory.OPERATOR),              // >
    GT("GT", TokenCategory.OPERATOR),              // >
    DASH("DASH", TokenCategory.OPERATOR),          // -
    ASTERISK("ASTERISK", TokenCategory.OPERATOR),  // *
    PLUS("PLUS", TokenCategory.OPERATOR),          // +
    LIST_NUMBER("LIST_NUM", TokenCategory.NUMBER),
    BACKSLASH("BACKSLASH", TokenCategory.OPERATOR),
    EQUALS("EQUALS", TokenCategory.OPERATOR),
    PIPE("PIPE", TokenCategory.OPERATOR),

    // Inline Delimiters
    UNDERSCORE("UNDERSCORE", TokenCategory.OPERATOR),
    BACKTICK("BACKTICK", TokenCategory.OPERATOR),
    BANG("BANG", TokenCategory.OPERATOR),          // !
    BRACKET_OPEN("BRACK_O", TokenCategory.OPERATOR),
    BRACKET_CLOSE("BRACK_C", TokenCategory.OPERATOR),
    PAREN_OPEN("PAREN_O", TokenCategory.OPERATOR),
    PAREN_CLOSE("PAREN_C", TokenCategory.OPERATOR),

    // Data
    TEXT("TEXT", TokenCategory.STRING),
    ESCAPED_CHAR("ESC", TokenCategory.STRING);     // e.g. \*

    private final String id;
    private final TokenCategory category;

    MarkdownTokenType(String id, TokenCategory category) {
        this.id = id;
        this.category = category;
    }

    @Override public String getId() { return id; }
    @Override public TokenCategory getCategory() { return category; }
}

// public enum MarkdownTokenType implements TokenType {
//     // Block types
//     HEADING_PREFIX("HEADING_PREFIX", TokenCategory.KEYWORD), // #, ##, etc.
//     LIST_BULLET("LIST_BULLET", TokenCategory.OPERATOR),      // -, *, +
//     BLOCKQUOTE_PREFIX("BLOCKQUOTE", TokenCategory.OPERATOR), // >

//     // Inline types
//     EMPHASIS_DELIMITER("EMPHASIS", TokenCategory.OPERATOR),  // * or _
//     CODE_FENCE("CODE", TokenCategory.STRING),              // ` or ```
//     LINK_OPEN("LINK_OPEN", TokenCategory.OPERATOR),          // [
//     LINK_CLOSE("LINK_CLOSE", TokenCategory.OPERATOR),        // ]

//     // Content
//     TEXT("TEXT", TokenCategory.STRING),
//     NEWLINE("NEWLINE", TokenCategory.WHITESPACE);

//     private final String id;
//     private final TokenCategory category;

//     MarkdownTokenType(String id, TokenCategory category) {
//         this.id = id;
//         this.category = category;
//     }

//     @Override public String getId() { return id; }
//     @Override public TokenCategory getCategory() { return category; }
// }