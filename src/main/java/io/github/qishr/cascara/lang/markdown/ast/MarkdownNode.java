package io.github.qishr.cascara.lang.markdown.ast;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.github.qishr.cascara.common.lang.ast.AstNode;
import io.github.qishr.cascara.common.lang.ast.CommentAstNode;

public abstract class MarkdownNode implements AstNode {

    protected int startLine, startColumn, endLine, endColumn;
    protected URI uri;
    protected List<MarkdownNode> children = new ArrayList<>();
    protected List<CommentAstNode> comments = new ArrayList<>();


    protected MarkdownNode() {
        startLine = 0;
        startColumn = 0;
        uri = null;
    }



    /// Constructs a new JsonNode with specific source coordinates.
    ///
    /// @param line   The 1-based line number in the source document.
    /// @param column The 1-based column number in the source document.
    /// @param uri    The URI of the source document.
    protected MarkdownNode(int line, int column, URI uri) {
        this.startLine = line;
        this.startColumn = column;
        this.uri = uri;
    }

    // Standard getters/setters for location data
    @Override public int getStartLine() { return startLine; }
    public void setStartLine(int line) { this.startLine = line; }

    @Override public int getStartColumn() { return startColumn; }
    public void setStartColumn(int column) { this.startColumn = column; }

    // Standard getters/setters for location data
    @Override public int getEndLine() { return endLine; }
    public void setEndLine(int line) { this.endLine = line; }

    @Override public int getEndColumn() { return endColumn; }
    public void setEndColumn(int column) { this.endColumn = column; }

    @Override public URI getUri() { return uri; }
    public void setUri(URI uri) { this.uri = uri; }

    // Each subclass (Object, Array, Scalar) handles its own children
    @Override public abstract List<MarkdownNode> getChildren();



    @Override
    public List<CommentAstNode> getComments() {
        throw new UnsupportedOperationException("Unimplemented method 'getComments'");
    }

    @Override
    public String getString() {
        List<MarkdownNode> children = getChildren();
        if (children.isEmpty()) return "";

        return children.stream()
                .map(MarkdownNode::getString)
                .collect(Collectors.joining());
    }
}