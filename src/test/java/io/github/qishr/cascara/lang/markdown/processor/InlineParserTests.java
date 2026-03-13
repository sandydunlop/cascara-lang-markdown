package io.github.qishr.cascara.lang.markdown.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.SimpleReporter;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.lang.markdown.MarkdownDocument;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownHeadingNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownLinkNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownParagraphNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownSequenceNode;

class InlineParserTests {
    @Test
    void testParagraphContinuity() {
        var parser = new MarkdownParser();
        String content = "Line one\nLine two\n\n# Heading";
        MarkdownDocument doc = parser.parse(content);

        MarkdownSequenceNode root = (MarkdownSequenceNode) doc.getRoot();

        // Should have 2 elements: 1 Paragraph and 1 Heading
        assertEquals(2, root.size());
        assertInstanceOf(MarkdownParagraphNode.class, root.get(0));
        assertTrue(root.get(1) instanceof MarkdownHeadingNode);
    }

    @Test
    void testLinkParsing() {
        java.util.Map<String, String> references = new java.util.HashMap<>();

        SimpleReporter reporter = new SimpleReporter().setLevel(Level.TRACE);
        var inlineParser = new InlineParser(reporter, references);
        var tokenizer = new MarkdownTokenizer();
        tokenizer.setReporter(reporter);
        var tokens = tokenizer.tokenize("[Cascara](https://cascara.io)");

        var nodes = inlineParser.parse(tokens);

        assertEquals(1, nodes.size());
        assertTrue(nodes.get(0) instanceof MarkdownLinkNode);
        assertEquals("https://cascara.io", ((MarkdownLinkNode)nodes.get(0)).getReferenceTarget());
        assertEquals("Cascara", nodes.get(0).getString());
    }
}