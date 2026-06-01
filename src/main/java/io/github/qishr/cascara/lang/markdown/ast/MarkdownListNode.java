package io.github.qishr.cascara.lang.markdown.ast;

import java.util.Iterator;
import java.util.List;

import io.github.qishr.cascara.common.lang.ast.CommentAstNode;
import io.github.qishr.cascara.common.lang.ast.SequenceAstNode;

public class MarkdownListNode extends MarkdownNode implements SequenceAstNode<MarkdownNode> {
    private final boolean ordered;

    public MarkdownListNode(boolean ordered) {
        this.ordered = ordered;
    }

    public boolean isOrdered() { return ordered; }

    // --- SequenceAstNode Implementation ---

    @Override
    public int size() { return children.size(); }

    @Override
    public MarkdownListNode add(MarkdownNode node) {
        if (!(node instanceof MarkdownListItemNode)) {
            throw new IllegalArgumentException("MarkdownListNode only accepts MarkdownListItemNode");
        }
        children.add((MarkdownNode) node);
        return this;
    }

    @Override
    public MarkdownNode get(int index) { return children.get(index); }

    @Override
    public List<MarkdownNode> getElements() { return children; }

    // @Override
    // public Iterable<MarkdownNode> items() { return children; }

    @Override
    public List<MarkdownNode> getChildren() { return children; }

    @Override
    public List<CommentAstNode> getComments() { return List.of(); }

    @Override
    public SequenceAstNode<MarkdownNode> remove(MarkdownNode node) {
        children.remove(node);
        return this;
    }

    @Override
    public MarkdownListNode remove(int index) {
        children.remove(index);
        return this;
    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'clear'");
    }

    /// Returns Iterator instance
    public Iterator<MarkdownNode> iterator() {
        return new SequenceIterator<MarkdownNode>(this);
    }

    static class SequenceIterator<T> implements Iterator<MarkdownNode> {
        MarkdownListNode list;
        int currentIndex = 0;

        // initialize pointer to head of the list for iteration
        public SequenceIterator(MarkdownListNode list) {
            this.list = list;
        }

        // returns false if next element does not exist
        public boolean hasNext() {
            return currentIndex < list.size();
        }

        // return current data and update pointer
        public MarkdownNode next() {
            MarkdownNode data = list.get(currentIndex++);
            return data;
        }

        // implement if needed
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
