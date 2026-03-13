package io.github.qishr.cascara.lang.markdown.ast;

public class MarkdownTableCellNode extends MarkdownSequenceNode {
    private final boolean isHeader;
    private Alignment alignment = Alignment.LEFT;

    public MarkdownTableCellNode(boolean isHeader) {
        this.isHeader = isHeader;
    }

    public void setAlignment(Alignment alignment) { this.alignment = alignment; }
    public Alignment getAlignment() { return alignment; }
    public boolean isHeader() { return isHeader; }
}
