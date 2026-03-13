package io.github.qishr.cascara.lang.markdown.ast;

public interface MarkdownVisitor {
    void visit(MarkdownTextNode node);
    void visit(MarkdownBoldNode node);
    void visit(MarkdownEmphasisNode node);
    void visit(MarkdownLinkNode node);
    void visit(MarkdownImageNode node);
    void visit(MarkdownCodeSpanNode node);
    void visit(MarkdownParagraphNode node);
    void visit(MarkdownHeadingNode node);
    // ... add other block types as you go
}