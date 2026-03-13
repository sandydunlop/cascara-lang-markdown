package io.github.qishr.cascara.lang.markdown.ast;

import java.util.List;

import io.github.qishr.cascara.common.lang.ast.CommentAstNode;
import io.github.qishr.cascara.common.lang.ast.SequenceAstNode;

public abstract class MarkdownSequenceNode extends MarkdownNode implements SequenceAstNode<MarkdownNode> {
    @Override public int size() { return children.size(); }
    @Override public void add(MarkdownNode node) { children.add((MarkdownNode)node); }
    @Override public void remove(int index) { children.remove(index); }
    @Override public void clear() { children.clear(); }
    @Override public MarkdownNode get(int index) { return children.get(index); }
    @Override public List<MarkdownNode> getElements() { return children; }
    @Override public Iterable<MarkdownNode> items() { return children; }
    @Override public List<MarkdownNode> getChildren() { return children; }

    @Override
    public List<CommentAstNode> getComments() {
        throw new UnsupportedOperationException("Unimplemented method 'getComments'");
    }
}