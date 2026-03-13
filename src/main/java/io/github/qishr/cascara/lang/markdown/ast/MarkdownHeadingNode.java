package io.github.qishr.cascara.lang.markdown.ast;

import java.util.List;

public class MarkdownHeadingNode extends MarkdownSequenceNode {
    private final int level;

    public MarkdownHeadingNode(int level) {
        this.level = level;
    }

    public int getLevel() { return level; }

    @Override
    public String getString() {
        // Concatenate children (text, bold, etc.) but ignore the '#' markers
        return super.getString();
    }

    @Override public List<MarkdownNode> getChildren() { return children; }
}