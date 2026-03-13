package io.github.qishr.cascara.lang.markdown.ast;

import java.util.ArrayList;
import java.util.List;

public class MarkdownTableRowNode extends MarkdownNode {
    private final List<MarkdownTableCellNode> cells = new ArrayList<>();

    public List<MarkdownTableCellNode> getCells() {
        return cells;
    }

    @Override
    public List<MarkdownNode> getChildren() {
        return new ArrayList<>(cells);
    }
}