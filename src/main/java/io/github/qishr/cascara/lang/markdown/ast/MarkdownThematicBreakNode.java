package io.github.qishr.cascara.lang.markdown.ast;

import java.util.Collections;
import java.util.List;

public class MarkdownThematicBreakNode extends MarkdownNode {
    @Override
    public List<MarkdownNode> getChildren() {
        return Collections.emptyList();
    }
}