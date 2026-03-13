package io.github.qishr.cascara.lang.markdown.ast;

import java.util.Collections;
import java.util.List;

public class MarkdownCodeSpanNode extends MarkdownNode {
    private final String code;

    public MarkdownCodeSpanNode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public String getContent() {
        return this.code;
    }

    @Override
    public List<MarkdownNode> getChildren() {
        // Code spans are terminal nodes in Markdown
        return Collections.emptyList();
    }
}