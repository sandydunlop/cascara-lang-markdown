package io.github.qishr.cascara.lang.markdown.ast;

import java.util.ArrayList;
import java.util.List;

public class MarkdownImageNode extends MarkdownNode {
    private final String url;
    private final String altText;
    private final List<MarkdownNode> children = new ArrayList<>();

    public MarkdownImageNode(String url, String altText) {
        this.url = url;
        this.altText = altText;
    }


    public String getUrl() { return url; }
    public String getAltText() { return altText; }

    @Override
    public List<MarkdownNode> getChildren() {
        return children;
    }
}