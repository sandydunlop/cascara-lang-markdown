package io.github.qishr.cascara.lang.markdown.ast;

import java.util.List;
import java.util.stream.Collectors;

import io.github.qishr.cascara.common.lang.ast.ReferenceAstNode;

import java.util.ArrayList;

public class MarkdownLinkNode extends MarkdownNode implements ReferenceAstNode<MarkdownNode> {
    private final String url;
    private final List<MarkdownNode> children = new ArrayList<>();

    public MarkdownLinkNode(String url) {
        this.url = url;
    }

    // For autolinks, you might want a helper or a constructor that adds the text node child
    public static MarkdownLinkNode createAutolink(String url) {
        MarkdownLinkNode link = new MarkdownLinkNode(url);
        link.getChildren().add(new MarkdownTextNode(url));
        return link;
    }

    public MarkdownLinkNode(String url, String displayText) {
        this(url);
        if (displayText != null) {
            this.getChildren().add(new MarkdownTextNode(displayText));
        }
    }

    @Override
    public String getReferenceTarget() {
        return url;
    }

    @Override
    public MarkdownNode resolve() {
        return null; // Standard for inline links
    }

    public String getUrl() {
        return url;
    }

    @Override
    public List<MarkdownNode> getChildren() {
        return children;
    }

    public void add(MarkdownNode node) {
        this.children.add(node);
    }

    @Override
    public String asString() {
        return getChildren().stream()
                .map(MarkdownNode::asString)
                .collect(Collectors.joining());
    }

}