package io.github.qishr.cascara.lang.markdown.token;

import io.github.qishr.cascara.common.lang.token.Token;

public record MarkdownToken(
    MarkdownTokenType type,
    String lexeme,
    String content,
    int offset,
    int startLine,
    int startColumn
) implements Token {
    @Override public MarkdownTokenType getType() { return type; }
    @Override public String getLexeme() { return lexeme; }
    @Override public String getContent() { return content; }
    @Override public int getOffset() { return offset; }
    @Override public int getStartLine() { return startLine; }
    @Override public int getStartColumn() { return startColumn; }
}