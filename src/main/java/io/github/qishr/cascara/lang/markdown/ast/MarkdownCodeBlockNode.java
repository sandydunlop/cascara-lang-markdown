package io.github.qishr.cascara.lang.markdown.ast;

import java.util.List;

public class MarkdownCodeBlockNode extends MarkdownNode {
    private final String content;
    private final String language; // For fenced blocks

    public MarkdownCodeBlockNode(String content, String language) {
        this.content = content;
        this.language = language;
    }

    public String getContent() { return content; }
    public String getLanguage() { return language; }

    @Override
    public List<MarkdownNode> getChildren() {
        return children;
    }
}