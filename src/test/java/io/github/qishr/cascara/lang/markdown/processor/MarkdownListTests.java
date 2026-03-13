package io.github.qishr.cascara.lang.markdown.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
import io.github.qishr.cascara.lang.markdown.ast.MarkdownLinkNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownListItemNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownListNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownParagraphNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownSequenceNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownTextNode;

class MarkdownListTests {

    MarkdownParser parser;

    @BeforeEach
    void init() {
        Reporter reporter = new SimpleReporter().setLevel(Level.TRACE);
        parser = new MarkdownParser();
        parser.setReporter(reporter);
    }

    @Test
    void testSimpleList() throws ParserException {
        String content = "* Item 1\n* Item 2";

        MarkdownDocument doc = parser.parse(content);
        MarkdownSequenceNode root = (MarkdownSequenceNode) doc.getRoot();

        // Should contain 1 List
        assertEquals(1, root.size());
        assertTrue(root.get(0) instanceof MarkdownListNode);

        MarkdownListNode list = (MarkdownListNode) root.get(0);
        assertEquals(2, list.size()); // Two items
    }

    @Test
    void testListWithFormatting() throws ParserException {
        String content = "* Item with **bold**";

        MarkdownDocument doc = parser.parse(content);
        MarkdownListNode list = (MarkdownListNode) ((MarkdownSequenceNode)doc.getRoot()).get(0);
        MarkdownListItemNode item = (MarkdownListItemNode) list.get(0);

        // The first block in the item should be a paragraph
        MarkdownSequenceNode itemContent = item;
        assertInstanceOf(MarkdownParagraphNode.class, itemContent.get(0));
    }

    @Test
    void testNestedList() throws ParserException {
        // Two-level nested list
        String content = """
            * Parent
              * Child
            """;

        MarkdownDocument doc = parser.parse(content);
        MarkdownSequenceNode root = (MarkdownSequenceNode) doc.getRoot();

        // 1. Root contains one list
        MarkdownListNode parentList = (MarkdownListNode) root.get(0);
        assertEquals(1, parentList.size());

        // 2. First Item of parent list contains a Paragraph AND a Nested List
        MarkdownListItemNode parentItem = (MarkdownListItemNode) parentList.get(0);
        MarkdownSequenceNode itemContent = parentItem;

        // itemContent should have: [ParagraphNode("Parent"), MarkdownListNode]
        assertTrue(itemContent.get(0) instanceof MarkdownParagraphNode, "First element should be the text 'Parent'");
        assertTrue(itemContent.get(1) instanceof MarkdownListNode, "Second element should be the nested child list");

        // 3. Check the child list
        MarkdownListNode childList = (MarkdownListNode) itemContent.get(1);
        assertEquals(1, childList.size());

        MarkdownListItemNode childItem = (MarkdownListItemNode) childList.get(0);
        // Verify child text
        MarkdownParagraphNode childText = (MarkdownParagraphNode) childItem.get(0);
        // (Assuming your scalar node stores text in a way we can verify)
    }

    @Test
    void testMixedOrderedNestedList() throws ParserException {
        // Unordered parent with an ordered child
        String content = """
            * Unordered
              1. Ordered
            """;

        MarkdownDocument doc = parser.parse(content);
        MarkdownListNode parentList = (MarkdownListNode) ((MarkdownSequenceNode)doc.getRoot()).get(0);
        MarkdownListItemNode parentItem = (MarkdownListItemNode) parentList.get(0);

        MarkdownListNode childList = (MarkdownListNode) parentItem.get(1);

        // Verify types
        assertTrue(!parentList.isOrdered(), "Parent list should be unordered (*)");
        assertTrue(childList.isOrdered(), "Child list should be ordered (1.)");
    }

    @Test
    void testDeeplyNestedList() throws ParserException {
        String content = """
            * Level 1
              * Level 2
                * Level 3
            """;

        MarkdownDocument doc = parser.parse(content);
        MarkdownSequenceNode root = (MarkdownSequenceNode) doc.getRoot();

        // Navigate to Level 3
        MarkdownListNode list1 = (MarkdownListNode) root.get(0);
        MarkdownListItemNode item1 = (MarkdownListItemNode) list1.get(0);

        MarkdownListNode list2 = (MarkdownListNode) item1.get(1);
        MarkdownListItemNode item2 = (MarkdownListItemNode) list2.get(0);

        MarkdownListNode list3 = (MarkdownListNode) item2.get(1);
        assertEquals(1, list3.size());

        MarkdownListItemNode item3 = (MarkdownListItemNode) list3.get(0);
        MarkdownParagraphNode textNode = (MarkdownParagraphNode) item3.get(0);
        // Verify Level 3 text exists
        assertTrue(textNode.getChildren().size() > 0);
    }

    @Test
    void testListThenParagraph() throws ParserException {
        // A list followed by a paragraph that is NOT indented
        String content = """
            * Item 1

            New Paragraph
            """;

        MarkdownDocument doc = parser.parse(content);
        MarkdownSequenceNode root = (MarkdownSequenceNode) doc.getRoot();

        // Should be [ListNode, MarkdownScalarNode]
        assertEquals(2, root.size());
        assertInstanceOf(MarkdownListNode.class, root.get(0));
        assertInstanceOf(MarkdownParagraphNode.class, root.get(1));
    }

    @Test
    void testAutolinks() {
        String input = "Contact <support@example.com> or visit <https://docs.qishr.io> or <div>";

        MarkdownDocument doc = parser.parse(input);
        MarkdownParagraphNode p = (MarkdownParagraphNode) doc.getChildren().get(0);

        List<MarkdownLinkNode> links = p.getChildren().stream()
                .filter(n -> n instanceof MarkdownLinkNode)
                .map(n -> (MarkdownLinkNode) n)
                .collect(Collectors.toList());

        assertEquals(2, links.size(), "Should have found exactly two autolinks");
        assertEquals("support@example.com", links.get(0).getUrl());
        assertEquals("https://docs.qishr.io", links.get(1).getUrl());

        // Ensure the <div> was treated as text, not a link
        boolean foundDiv = p.getChildren().stream()
                .anyMatch(n -> n instanceof MarkdownTextNode && ((MarkdownTextNode)n).getText().contains("div"));
        assertTrue(foundDiv, "The <div> tag should remain as text if not a valid URL/email");
    }

    @Test
    void testTaskLists() {
        String input = """
                - [ ] Todo
                - [x] Done
                - Standard item""";

        MarkdownDocument doc = parser.parse(input);
        MarkdownListNode list = (MarkdownListNode) doc.getChildren().get(0);

        MarkdownListItemNode item1 = (MarkdownListItemNode) list.getChildren().get(0);
        assertTrue(item1.isTask());
        assertFalse(item1.isCompleted());

        MarkdownListItemNode item2 = (MarkdownListItemNode) list.getChildren().get(1);
        assertTrue(item2.isTask());
        assertTrue(item2.isCompleted());

        MarkdownListItemNode item3 = (MarkdownListItemNode) list.getChildren().get(2);
        assertFalse(item3.isTask());
    }
}