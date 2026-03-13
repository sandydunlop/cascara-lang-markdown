package io.github.qishr.cascara.lang.markdown.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.SimpleReporter;
import io.github.qishr.cascara.common.lang.exception.ParserException;
import io.github.qishr.cascara.lang.markdown.MarkdownDocument;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownBlockquoteNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownHeadingNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownLineBreakNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownListItemNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownListNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownParagraphNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownSequenceNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownTextNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownThematicBreakNode;
import io.github.qishr.cascara.lang.markdown.token.MarkdownToken;
import io.github.qishr.cascara.lang.markdown.token.MarkdownTokenType;

class MarkdownParserTests {
    MarkdownParser parser;

    @BeforeEach
    void init() {
        Reporter reporter = new SimpleReporter().setLevel(Level.TRACE);
        parser = new MarkdownParser();
        parser.setReporter(reporter);
    }

    @Test
    void testNestedBlockquote() throws ParserException {
        MarkdownParser parser = new MarkdownParser();
        String content = "> # Heading inside quote\n> This is a paragraph.";

        MarkdownDocument doc = parser.parse(content);

        // Document IS the sequence now - no getRoot() needed
        assertEquals(1, doc.size());
        assertTrue(doc.get(0) instanceof MarkdownBlockquoteNode);

        // Blockquote IS the sequence now - no getContent() needed
        MarkdownBlockquoteNode quote = (MarkdownBlockquoteNode) doc.get(0);

        assertEquals(2, quote.size());
        assertInstanceOf(MarkdownHeadingNode.class, quote.get(0));
        assertInstanceOf(MarkdownParagraphNode.class, quote.get(1));
    }

    @Test
    void testParagraphTermination() throws ParserException {
        MarkdownParser parser = new MarkdownParser();
        String content = "Paragraph one.\n\n# Heading";

        MarkdownDocument doc = parser.parse(content);
        MarkdownSequenceNode root = (MarkdownSequenceNode) doc.getRoot();

        assertEquals(2, root.size());
        assertInstanceOf(MarkdownParagraphNode.class, root.get(0));
        assertInstanceOf(MarkdownHeadingNode.class, root.get(1));
    }

    @Test
    void testReferenceMapPopulation() throws ParserException {
        MarkdownParser parser = new MarkdownParser();
        String content = "[1]: http://google.com \"Google\"";

        parser.parse(content);

        // We need to verify the map was populated.
        // Assuming you add: public String getReference(String id) { return references.get(id); }
        assertEquals("http://google.com", parser.getReference("1"));
    }

    @Test
    void testIsLinkDefinitionDirectly() {
        MarkdownParser parser = new MarkdownParser();

        // Simulate tokens for: [1] : http://google.com
        List<MarkdownToken> tokens = List.of(
            new MarkdownToken(MarkdownTokenType.BRACKET_OPEN, "[", null, 0, 1, 1),
            new MarkdownToken(MarkdownTokenType.TEXT, "1", null, 1, 1, 2),
            new MarkdownToken(MarkdownTokenType.BRACKET_CLOSE, "]", null, 2, 1, 3),
            new MarkdownToken(MarkdownTokenType.WHITESPACE, " ", null, 3, 1, 4), // The likely culprit
            new MarkdownToken(MarkdownTokenType.COLON, ":", null, 4, 1, 5)
        );

        // Inject tokens into parser manually for the test
        setParserTokens(parser, tokens);

        assertTrue(parser.isLinkDefinition(),
            "Parser should recognize link definition even with whitespace before the colon");
    }

    // Helper to set private state for the test
    private void setParserTokens(MarkdownParser parser, List<MarkdownToken> tokens) {
        try {
            var fTokens = MarkdownParser.class.getDeclaredField("tokens");
            fTokens.setAccessible(true);
            fTokens.set(parser, tokens);

            var fIndex = MarkdownParser.class.getDeclaredField("index");
            fIndex.setAccessible(true);
            fIndex.set(parser, 0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testIsLinkDefinitionWithRealTokenizer() {
        MarkdownParser parser = new MarkdownParser();
        MarkdownTokenizer tokenizer = new MarkdownTokenizer();

        // Use the exact format from your failing trace
        String input = "[1]: http://google.com";
        List<MarkdownToken> tokens = tokenizer.tokenize(input);

        // Inject the real tokens
        setParserTokens(parser, tokens);

        // If this fails, we've caught the bug in the wild.
        assertTrue(parser.isLinkDefinition(),
            "isLinkDefinition failed on real tokens: " + tokens.stream()
                .map(t -> "[" + t.getType() + ":" + t.getLexeme() + "]")
                .collect(Collectors.joining(", ")));
    }

    @Test
    void testIsLinkDefinitionWithWhitespaceVariations() {
        MarkdownParser parser = new MarkdownParser();
        MarkdownTokenizer tokenizer = new MarkdownTokenizer();

        // Variation 2: Leading newline
        String input2 = "\n[1]: http://google.com";
        List<MarkdownToken> tokens2 = tokenizer.tokenize(input2);
        setParserTokens(parser, tokens2);

        // Simulate the parseBlocks loop logic: skip the newline
        if (tokens2.get(0).getType() == MarkdownTokenType.NEWLINE) {
            advanceIndex(parser); // You'll need a helper to increment the 'index' field
        }

        assertTrue(parser.isLinkDefinition(), "Should find definition after skipping leading newline");
    }

    private void advanceIndex(MarkdownParser parser) {
        try {
            var fIndex = MarkdownParser.class.getDeclaredField("index");
            fIndex.setAccessible(true);
            int current = (int) fIndex.get(parser);
            fIndex.set(parser, current + 1);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void testThematicBreaks() {
        String input = "***\n---\n___ \n--- with text";

        MarkdownDocument doc = parser.parse(input);

        // We expect 3 thematic breaks and 1 paragraph
        long breakCount = doc.getChildren().stream()
                .filter(n -> n instanceof MarkdownThematicBreakNode)
                .count();

        assertEquals(3, breakCount);

        MarkdownParagraphNode p = (MarkdownParagraphNode) doc.getChildren().get(3);
        MarkdownTextNode text = (MarkdownTextNode) p.getChildren().get(0);
        assertTrue(text.getText().contains("--- with text"));
    }

    @Test
    void testBreaksAndSetext() {
        String input = "Line One  \nLine Two\\\nLine Three\n\nHeading 1\n=====\n\nHeading 2\n-----";

        MarkdownDocument doc = parser.parse(input);

        // Paragraph with two breaks
        MarkdownParagraphNode p = (MarkdownParagraphNode) doc.getChildren().get(0);
        long breakCount = p.getChildren().stream().filter(n -> n instanceof MarkdownLineBreakNode).count();
        assertEquals(2, breakCount);

        // Setext H1
        MarkdownHeadingNode h1 = (MarkdownHeadingNode) doc.getChildren().get(1);
        assertEquals(1, h1.getLevel());

        // Setext H2
        MarkdownHeadingNode h2 = (MarkdownHeadingNode) doc.getChildren().get(2);
        assertEquals(2, h2.getLevel());
    }

    @Test
    public void testListNestingDepth() {
        String markdown = """
            - Tree view
              - Tree starts
              - Folders are hierarchical
            - Root sibling
            """;

        // 1. Parse the document
        // MarkdownParser parser = new MarkdownParser();
        MarkdownDocument doc = parser.parse(markdown);

        // 2. Debug Dump (so you can see it in the test logs)
        System.out.println("AST Dump:\n" + MarkdownAstDumper.dump(doc));

        // 3. Assertions
        // We expect ONE root list
        assertEquals(1, doc.getChildren().size());
        MarkdownListNode rootList = (MarkdownListNode) doc.getChildren().get(0);

        // Root list should have TWO items: "Tree view" and "Root sibling"
        assertEquals(2, rootList.getChildren().size(), "Root list should have 2 sibling items");

        // Get the first item ("Tree view")
        MarkdownListItemNode item1 = (MarkdownListItemNode) rootList.getChildren().get(0);

        // Item 1 should contain ONE sub-list
        MarkdownListNode subList = findFirstChildOf(item1, MarkdownListNode.class);
        assertNotNull(subList, "Item 1 should have a nested sub-list");

        // CRITICAL: The sub-list should have TWO siblings (the bug was making them nested)
        assertEquals(2, subList.getChildren().size(),
            "The sub-list should have 2 siblings ('Tree starts' and 'Folders are')");
    }

    /**
     * Helper to find a specific node type within a parent's children
     */
    private <T extends MarkdownNode> T findFirstChildOf(MarkdownNode parent, Class<T> clazz) {
        return parent.getChildren().stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .findFirst()
                .orElse(null);
    }

    @Test
    public void testIndentDetection() {
        // Case A: No indent
        // "- Item" -> tokens: [DASH, WHITESPACE, TEXT]

        // Case B: 2-space indent
        // "  - Item" -> tokens: [WHITESPACE(len=2), DASH, WHITESPACE, TEXT]

        // Case C: 4-space indent (often lexed as INDENT token)
        // "    - Item" -> tokens: [INDENT(len=4), DASH, WHITESPACE, TEXT]
    }

    @Test
    public void testListLookahead() {
        // Should return true if index points to:
        // [WHITESPACE, DASH, WHITESPACE]
        // [DASH, WHITESPACE]
        // [WHITESPACE, LIST_NUMBER, WHITESPACE]
    }
}