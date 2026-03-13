package io.github.qishr.cascara.lang.markdown.ast;

import java.util.List;

/// Represents a blockquote container.
///
/// By extending MarkdownSequenceNode, it directly contains the
/// blocks (paragraphs, etc.) that make up the quote.
public class MarkdownBlockquoteNode extends MarkdownSequenceNode {

    @Override
    public List<MarkdownNode> getChildren() {
        // Now it uses the internal list from MarkdownSequenceNode
        return this.children;
    }
}