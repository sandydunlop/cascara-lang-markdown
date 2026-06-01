package io.github.qishr.cascara.lang.markdown.ast;

import java.util.Iterator;
import java.util.List;

import io.github.qishr.cascara.common.lang.ast.CommentAstNode;
import io.github.qishr.cascara.common.lang.ast.SequenceAstNode;

public abstract class MarkdownSequenceNode extends MarkdownNode implements SequenceAstNode<MarkdownNode> {
    @Override public int size() { return children.size(); }
    @Override public MarkdownSequenceNode add(MarkdownNode node) { children.add((MarkdownNode)node); return this; }
    @Override public MarkdownSequenceNode remove(int index) { children.remove(index); return this; }
    @Override public void clear() { children.clear(); }
    @Override public MarkdownNode get(int index) { return children.get(index); }
    @Override public List<MarkdownNode> getElements() { return children; }
    // @Override public Iterable<MarkdownNode> items() { return children; }
    @Override public List<MarkdownNode> getChildren() { return children; }

    @Override
    public SequenceAstNode<MarkdownNode> remove(MarkdownNode node) {
        children.remove(node);
        return this;
    }

    @Override
    public List<CommentAstNode> getComments() {
        throw new UnsupportedOperationException("Unimplemented method 'getComments'");
    }
    /// Returns Iterator instance
    public Iterator<MarkdownNode> iterator() {
        return new SequenceIterator<MarkdownNode>(this);
    }

    static class SequenceIterator<T> implements Iterator<MarkdownNode> {
        MarkdownSequenceNode list;
        int currentIndex = 0;

        // initialize pointer to head of the list for iteration
        public SequenceIterator(MarkdownSequenceNode list) {
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