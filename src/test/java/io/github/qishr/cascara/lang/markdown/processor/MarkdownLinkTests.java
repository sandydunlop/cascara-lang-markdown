package io.github.qishr.cascara.lang.markdown.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.SimpleReporter;
import io.github.qishr.cascara.common.lang.exception.ParserException;
import io.github.qishr.cascara.lang.markdown.MarkdownDocument;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownBoldNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownEmphasisNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownLinkNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownParagraphNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownSequenceNode;

public class MarkdownLinkTests {
    MarkdownParser parser;

    @BeforeEach
    void init() {
        Reporter reporter = new SimpleReporter().setLevel(Level.TRACE);
        parser = new MarkdownParser();
        parser.setReporter(reporter);
    }

    @Test
    void testLinkWithFormatting() throws ParserException {
        // A link containing bold text
        String content = "[The **Cascara** Project](https://github.com/qishr/cascara)";

        MarkdownDocument doc = parser.parse(content);
        MarkdownSequenceNode root = (MarkdownSequenceNode) doc.getRoot();
        MarkdownParagraphNode paragraph = (MarkdownParagraphNode) root.get(0);

        // The paragraph should contain one child: the LinkNode
        assertTrue(paragraph.getChildren().get(0) instanceof MarkdownLinkNode);

        MarkdownLinkNode link = (MarkdownLinkNode) paragraph.getChildren().get(0);
        assertEquals("https://github.com/qishr/cascara", link.getUrl());

        // Check nested formatting in label: [TEXT("The "), BOLD("Cascara"), TEXT(" Project")]
        List<MarkdownNode> labelChildren = link.getChildren();
        assertEquals(3, labelChildren.size());
        assertTrue(labelChildren.get(1) instanceof MarkdownBoldNode ||
                labelChildren.get(1) instanceof MarkdownEmphasisNode);
    }

    @Test
    void testComplexLink() throws ParserException {
        String content = "Check [this **important** link](https://google.com)";

        MarkdownDocument doc = parser.parse(content);
        MarkdownSequenceNode root = (MarkdownSequenceNode) doc.getRoot();
        assertInstanceOf(MarkdownParagraphNode.class, root.get(0));
        MarkdownParagraphNode p = (MarkdownParagraphNode) root.get(0);

        // Children: TEXT("Check "), LINK(...)
        assertTrue(p.getChildren().get(1) instanceof MarkdownLinkNode);

        MarkdownLinkNode link = (MarkdownLinkNode) p.getChildren().get(1);
        assertEquals("https://google.com", link.getUrl());

        // Check for nested bold in the link label
        // label children: [TEXT("this "), BOLD("important"), TEXT(" link")]
        boolean hasBold = link.getChildren().stream()
            .anyMatch(n -> n instanceof MarkdownEmphasisNode); // or MarkdownBoldNode
        assertTrue(hasBold, "Link label should contain an emphasis node");
    }

    @Test
    void testReferenceLinks() throws ParserException {
        String content = """
            [Google][1] and [Yahoo].

            [1]: http://google.com "Google Search"
            [Yahoo]: http://yahoo.com
            """;

        MarkdownDocument doc = parser.parse(content);

        // The reference definitions should be consumed as metadata,
        // leaving only 1 paragraph.
        assertEquals(1, doc.size());
        MarkdownParagraphNode p = (MarkdownParagraphNode) doc.get(0);

        // Extract links from paragraph
        List<MarkdownLinkNode> links = p.getChildren().stream()
            .filter(n -> n instanceof MarkdownLinkNode)
            .map(n -> (MarkdownLinkNode) n)
            .toList();

        assertEquals(2, links.size());
        assertEquals("http://google.com", links.get(0).getUrl());
        assertEquals("http://yahoo.com", links.get(1).getUrl());
    }
}
