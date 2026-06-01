package io.github.qishr.cascara.lang.markdown.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.SimpleReporter;
import io.github.qishr.cascara.lang.markdown.MarkdownDocument;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownImageNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownParagraphNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownTextNode;

public class MarkdownImagesTests {

    MarkdownParser parser;

    @BeforeEach
    void init() {
        Reporter reporter = new SimpleReporter().setLevel(Level.TRACE);
        parser = new MarkdownParser();
        parser.setReporter(reporter);
    }

    @Test
    void testImages() {
        MarkdownParser parser = new MarkdownParser();
        String content = "![Alt text](img.png) and ![Ref image][img2]\n\n[img2]: photo.jpg";

        MarkdownDocument doc = parser.parse(content);
        MarkdownParagraphNode p = (MarkdownParagraphNode) doc.get(0);

        long imageCount = p.getChildren().stream()
            .filter(n -> n instanceof MarkdownImageNode)
            .count();

        assertEquals(2, imageCount);

        MarkdownImageNode img1 = (MarkdownImageNode) p.getChildren().get(0);
        assertEquals("img.png", img1.getUrl());
    }

    @Test
    void testImages2() {
        String input = "Check this: ![Logo](https://qishr.io/logo.png) and a literal ! mark.";

        MarkdownDocument doc = parser.parse(input);
        MarkdownParagraphNode p = (MarkdownParagraphNode) doc.getChildren().get(0);

        MarkdownImageNode image = p.getChildren().stream()
                .filter(n -> n instanceof MarkdownImageNode)
                .map(n -> (MarkdownImageNode) n)
                .findFirst()
                .orElseThrow();

        assertEquals("https://qishr.io/logo.png", image.getUrl());
        assertEquals("Logo", image.getAltText());

        // Verify the second '!' is just text
        boolean hasLiteralExclamation = p.getChildren().stream()
                .anyMatch(n -> n instanceof MarkdownTextNode && ((MarkdownTextNode)n).getText().contains("!"));
        assertTrue(hasLiteralExclamation);
    }

    @Test
    void testReferenceImages() {
        String input = "![The Logo][logo]\n\n[logo]: https://qishr.io/img.png";

        MarkdownDocument doc = parser.parse(input);
        MarkdownParagraphNode p = (MarkdownParagraphNode) doc.getChildren().get(0);

        MarkdownImageNode img = (MarkdownImageNode) p.getChildren().get(0);
        assertEquals("https://qishr.io/img.png", img.getUrl());
        assertEquals("The Logo", img.getAltText());
    }
}
