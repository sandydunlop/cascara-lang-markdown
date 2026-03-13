package io.github.qishr.cascara.lang.markdown.processor;

import io.github.qishr.cascara.lang.markdown.ast.MarkdownCodeSpanNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownHeadingNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownLinkNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownListItemNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownTextNode;

public class MarkdownAstDumper {

    public static String dump(MarkdownNode root) {
        StringBuilder sb = new StringBuilder();
        dumpNode(root, sb, 0);
        return sb.toString();
    }

    private static void dumpNode(MarkdownNode node, StringBuilder sb, int depth) {
        // Indentation for the tree structure
        sb.append("  ".repeat(depth));

        // Node Type and basic info
        sb.append("[").append(node.getClass().getSimpleName()).append("]");

        // Add specific details based on node type
        if (node instanceof MarkdownTextNode n) {
            sb.append(" text=\"").append(n.getText()).append("\"");
        } else if (node instanceof MarkdownHeadingNode n) {
            sb.append(" level=").append(n.getLevel());
        } else if (node instanceof MarkdownListItemNode n) {
            sb.append(" (task=").append(n.isTask())
              .append(", checked=").append(n.isCompleted()).append(")");
        } else if (node instanceof MarkdownLinkNode n) {
            sb.append(" url=").append(n.getUrl());
        } else if (node instanceof MarkdownCodeSpanNode n) {
            sb.append(" content=\"").append(n.getContent()).append("\"");
        }

        sb.append("\n");

        // Recurse into children
        if (node.getChildren() != null) {
            for (MarkdownNode child : node.getChildren()) {
                dumpNode(child, sb, depth + 1);
            }
        }
    }
}