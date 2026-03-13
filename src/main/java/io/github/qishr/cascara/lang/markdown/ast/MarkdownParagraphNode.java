package io.github.qishr.cascara.lang.markdown.ast;

import java.util.List;

/**
 * Represents a paragraph block in a Markdown document.
 * A paragraph is a leaf block that contains inline content.
 */
public class MarkdownParagraphNode extends MarkdownSequenceNode {

    @Override
    public List<MarkdownNode> getChildren() {
        return this.children;
    }
}