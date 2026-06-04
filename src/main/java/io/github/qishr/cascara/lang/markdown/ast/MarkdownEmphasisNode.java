package io.github.qishr.cascara.lang.markdown.ast;

import java.util.ArrayList;
import java.util.List;

public class MarkdownEmphasisNode extends MarkdownNode {
    private final int delimiterCount; // 1 for *, 2 for **
    private final List<MarkdownNode> children = new ArrayList<>();

    public MarkdownEmphasisNode(int delimiterCount) {
        this.delimiterCount = delimiterCount;
    }

    public int getDelimiterCount() {
        return delimiterCount;
    }

    @Override
    public String asString() {
        // Return the inner text without the formatting markers
        return super.asString();
    }


    @Override
    public List<MarkdownNode> getChildren() {
        return children;
    }

    public boolean isBold() {
        // Usually determined by whether the delimiter was ** or __
        // If you store the 'level' or 'count' of delimiters:
        return this.delimiterCount >= 2;
    }

    /**
     * Allows the InlineParser to add nodes (Text, Links, etc.)
     * discovered between the emphasis delimiters.
     */
    public void add(MarkdownNode node) {
        this.children.add(node);
    }
}