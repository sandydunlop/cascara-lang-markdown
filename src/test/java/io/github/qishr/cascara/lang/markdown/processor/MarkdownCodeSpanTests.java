package io.github.qishr.cascara.lang.markdown.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.SimpleReporter;
import io.github.qishr.cascara.lang.markdown.MarkdownDocument;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownCodeSpanNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownParagraphNode;

public class MarkdownCodeSpanTests {

    MarkdownParser parser;

    @BeforeEach
    void init() {
        Reporter reporter = new SimpleReporter().setLevel(Level.TRACE);
        parser = new MarkdownParser();
        parser.setReporter(reporter);
    }

    @Test
    void testCodeSpans() {
        // MarkdownParser parser = new MarkdownParser();
        // 1. Standard code span
        // 2. Space normalization: `  code  ` -> ` code `
        // 3. Double backticks to allow single backticks inside
        String input = "Try `printf()`, `` ` `` and `  spaced  `.";

        MarkdownDocument doc = parser.parse(input);
        MarkdownParagraphNode p = (MarkdownParagraphNode) doc.getChildren().get(0);

        List<MarkdownCodeSpanNode> codeNodes = p.getChildren().stream()
                .filter(n -> n instanceof MarkdownCodeSpanNode)
                .map(n -> (MarkdownCodeSpanNode) n)
                .collect(Collectors.toList());

        assertEquals(3, codeNodes.size());
        assertEquals("printf()", codeNodes.get(0).getCode());
        assertEquals("`", codeNodes.get(1).getCode());
        assertEquals(" spaced ", codeNodes.get(2).getCode());
    }

    @Test
    void testCodeSpans2() {
        // Normal, double-delimiter with nested single, and triple-delimiter
        String input = "Use `printf()`, `` ` `` and ``` `` ```.";

        MarkdownDocument doc = parser.parse(input);
        MarkdownParagraphNode p = (MarkdownParagraphNode) doc.getChildren().get(0);

        List<MarkdownCodeSpanNode> codeNodes = p.getChildren().stream()
                .filter(n -> n instanceof MarkdownCodeSpanNode)
                .map(n -> (MarkdownCodeSpanNode) n)
                .collect(Collectors.toList());

        assertEquals(3, codeNodes.size());
        assertEquals("printf()", codeNodes.get(0).getCode());
        assertEquals("`", codeNodes.get(1).getCode());
        assertEquals("``", codeNodes.get(2).getCode());
    }

    @Test
    void testNestedBackticks() {
        // Input has a code span delimited by TWO backticks
        // It contains a single backtick as literal text.
        String input = "Here is a backtick: `` ` ``";

        MarkdownDocument doc = parser.parse(input);
        MarkdownParagraphNode p = (MarkdownParagraphNode) doc.getChildren().get(0);

        MarkdownCodeSpanNode codeNode = p.getChildren().stream()
                .filter(n -> n instanceof MarkdownCodeSpanNode)
                .map(n -> (MarkdownCodeSpanNode) n)
                .findFirst()
                .orElseThrow();

        // The result should be just the backtick.
        // The outer spaces are stripped per CommonMark rules for `` ` ``
        assertEquals("`", codeNode.getCode());
    }

    @Test
    void testCodeSpanSpaceNormalization() {
        // 1. Double backticks with spaces to wrap a single backtick
        // 2. Spaces that should be preserved (only one on each side is removed)
        // 3. Single space (should not be trimmed if it's the only content)
        String input = "Fixed: `` ` ``, Extended: `  spaced  `, Minimal: ` `";

        MarkdownDocument doc = parser.parse(input);
        MarkdownParagraphNode p = (MarkdownParagraphNode) doc.getChildren().get(0);

        List<MarkdownCodeSpanNode> codeNodes = p.getChildren().stream()
                .filter(n -> n instanceof MarkdownCodeSpanNode)
                .map(n -> (MarkdownCodeSpanNode) n)
                .collect(Collectors.toList());

        assertEquals(3, codeNodes.size());

        // `` ` `` -> Strips the outer spaces, leaves the backtick
        assertEquals("`", codeNodes.get(0).getCode());

        // `  spaced  ` -> Strips one space from each side, leaves " spaced "
        assertEquals(" spaced ", codeNodes.get(1).getCode());

        // ` ` -> If it's just a space, it is usually preserved as a single space
        assertEquals(" ", codeNodes.get(2).getCode());
    }

    @Test
    void testCodeSpanSpaceNormalization2() {
        String input = "`` ` `` and `  spaced  `";
        MarkdownDocument doc = parser.parse(input);
        MarkdownParagraphNode p = (MarkdownParagraphNode) doc.getChildren().get(0);

        List<MarkdownCodeSpanNode> nodes = p.getChildren().stream()
                .filter(n -> n instanceof MarkdownCodeSpanNode)
                .map(n -> (MarkdownCodeSpanNode) n)
                .toList();

        assertEquals("`", nodes.get(0).getCode(), "Double backticks should strip outer spaces");
        assertEquals(" spaced ", nodes.get(1).getCode(), "Should only strip one space from each side");
    }

    @Test
    void testCodeSpanWithOnlySpaces() {
        // 1. One space: should remain " "
        // 2. Two spaces: should remain "  "
        // 3. Three spaces: should remain "   "
        String input = "Single: ` `, Double: `  `, Triple: `   `";

        MarkdownDocument doc = parser.parse(input);
        MarkdownParagraphNode p = (MarkdownParagraphNode) doc.getChildren().get(0);

        List<MarkdownCodeSpanNode> nodes = p.getChildren().stream()
                .filter(n -> n instanceof MarkdownCodeSpanNode)
                .map(n -> (MarkdownCodeSpanNode) n)
                .toList();

        assertEquals(3, nodes.size());
        assertEquals(" ", nodes.get(0).getCode(), "Single space should not be stripped");
        assertEquals("  ", nodes.get(1).getCode(), "Double space should not be stripped");
        assertEquals("   ", nodes.get(2).getCode(), "Triple space should not be stripped");
    }
}
