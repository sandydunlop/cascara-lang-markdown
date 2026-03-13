package io.github.qishr.cascara.lang.markdown.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.SimpleReporter;
import io.github.qishr.cascara.lang.markdown.MarkdownDocument;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownCodeBlockNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownParagraphNode;

public class MarkdownCodeBlockTests {

    MarkdownParser parser;

    @BeforeEach
    void init() {
        Reporter reporter = new SimpleReporter().setLevel(Level.TRACE);
        parser = new MarkdownParser();
        parser.setReporter(reporter);
    }

    @Test
    void testCodeBlocks() {
        String input = "    var x = *not italic*;\n\n```java\nSystem.out.println(\"Hello\");\n```";

        MarkdownDocument doc = parser.parse(input);

        // 1. Indented Block
        MarkdownCodeBlockNode indented = (MarkdownCodeBlockNode) doc.getChildren().get(0);
        assertTrue(indented.getContent().contains("*not italic*"), "Indented code should be literal");

        // 2. Fenced Block
        MarkdownCodeBlockNode fenced = (MarkdownCodeBlockNode) doc.getChildren().get(1);
        assertEquals("java", fenced.getLanguage());
        assertTrue(fenced.getContent().contains("System.out.println"));
    }

    @Test
    void testCodeBlockBoundaries() {
        // Test 1: Indented code should NOT be part of the preceding paragraph
        // Test 2: Fenced code should NOT be part of the preceding paragraph
        // Test 3: Paragraph continuity should still work for normal lines
        String input = """
                This is a normal paragraph
                that continues here.
                    this_is_indented_code();

                ```java
                this_is_fenced_code();
                ```

                Back to paragraph.""";

        MarkdownDocument doc = parser.parse(input);

        // We expect: Paragraph, CodeBlock (Indented), CodeBlock (Fenced), Paragraph
        assertEquals(4, doc.getChildren().size(), "Should have exactly 4 blocks");

        // Check first paragraph continuity
        MarkdownParagraphNode p1 = (MarkdownParagraphNode) doc.getChildren().get(0);
        // Should contain "This is a normal paragraph" + "that continues here."
        // If it incorrectly swallowed the code, the child count or text would be wrong.

        assertTrue(doc.getChildren().get(1) instanceof MarkdownCodeBlockNode, "Second block must be Indented Code");
        assertTrue(doc.getChildren().get(2) instanceof MarkdownCodeBlockNode, "Third block must be Fenced Code");
    }

    @Test
    void testIndentedCodeLiteralness() {
        // Characters like * and _ should remain literal in code blocks
        String input = "    System.out.println(*not_italic*);";
        MarkdownDocument doc = parser.parse(input);

        MarkdownCodeBlockNode code = (MarkdownCodeBlockNode) doc.getChildren().get(0);
        assertEquals("System.out.println(*not_italic*);", code.getContent().trim());
    }
}
