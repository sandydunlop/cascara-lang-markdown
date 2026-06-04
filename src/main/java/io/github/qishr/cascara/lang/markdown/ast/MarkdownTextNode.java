package io.github.qishr.cascara.lang.markdown.ast;

import java.util.Collections;
import java.util.List;

public class MarkdownTextNode extends MarkdownNode {
    private final String text;
    public MarkdownTextNode(String text) { this.text = text; }

    @Override
    public List<MarkdownNode> getChildren() {
        return Collections.emptyList();
    }

    public String getText() {
        return text;
    }

    @Override
    public String asString() {
        return text; // Leaf actually provides the raw data
    }
}