package io.github.qishr.cascara.lang.markdown;

import java.net.URI;
import java.util.List;

import io.github.qishr.cascara.common.lang.StructuredDocument;
import io.github.qishr.cascara.common.lang.ast.CommentAstNode;
import io.github.qishr.cascara.common.lang.ast.SequenceAstNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownSequenceNode;

/// Represents a complete Markdown document.
///
/// Since it extends MarkdownSequenceNode, it acts as the primary container
/// for top-level block elements.
public class MarkdownDocument extends MarkdownSequenceNode implements StructuredDocument {
    private URI schemaUri = null;

    public MarkdownDocument() {
        this(null);
    }

    public MarkdownDocument(URI uri) {
        super();
        this.schemaUri = uri;
    }

    /// Returns the primary content node of the document.
    @Override
    public MarkdownNode getRoot() { return this; }

    /// {@inheritDoc}
    // @Override public List<? extends AstNode> getChildren() { return children; }

    //
    // StructuredDocument Implementation
    //

    @Override public URI getSchemaUri() {
        return schemaUri;
    }

    @Override
    public List<CommentAstNode> getComments() {
        throw new UnsupportedOperationException("Unimplemented method 'getComments'");
    }

    @Override
    public SequenceAstNode<MarkdownNode> remove(MarkdownNode node) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'remove'");
    }

}