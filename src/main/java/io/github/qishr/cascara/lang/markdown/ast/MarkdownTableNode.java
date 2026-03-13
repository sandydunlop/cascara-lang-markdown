package io.github.qishr.cascara.lang.markdown.ast;

import java.util.ArrayList;
import java.util.List;

public class MarkdownTableNode extends MarkdownNode {
    private final List<MarkdownTableRowNode> rows = new ArrayList<>();

    public void addRow(MarkdownTableRowNode row) {
        rows.add(row);
    }

    public List<MarkdownTableRowNode> getRows() {
        return rows;
    }

    @Override
    public List<MarkdownNode> getChildren() {
        return new ArrayList<>(rows);
    }
}