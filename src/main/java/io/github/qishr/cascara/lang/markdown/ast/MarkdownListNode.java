package io.github.qishr.cascara.lang.markdown.ast;

import java.util.List;

import io.github.qishr.cascara.common.lang.ast.AstNode;
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
    public void add(MarkdownNode node) {
        if (!(node instanceof MarkdownListItemNode)) {
            throw new IllegalArgumentException("MarkdownListNode only accepts MarkdownListItemNode");
        }
        children.add((MarkdownNode) node);
    }

    @Override
    public MarkdownNode get(int index) { return children.get(index); }

    @Override
    public List<MarkdownNode> getElements() { return children; }

    @Override
    public Iterable<MarkdownNode> items() { return children; }

    @Override
    public List<MarkdownNode> getChildren() { return children; }

    @Override
    public List<CommentAstNode> getComments() { return List.of(); }

    @Override
    public void remove(int index) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'remove'");
    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'clear'");
    }
}

// public class MarkdownListNode extends MarkdownNode {
//     private final boolean ordered;

//     public MarkdownListNode(boolean ordered) {
//         this.ordered = ordered;
//     }

//     public boolean isOrdered() { return ordered; }

//     /**
//      * Helper to allow the parser to add items specifically.
//      */
//     public void add(MarkdownListItemNode item) {
//         this.children.add(item);
//     }

//     @Override
//     public List<MarkdownNode> getChildren() {
//         return this.children; // Inherited from MarkdownNode
//     }
// }

