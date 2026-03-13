package io.github.qishr.cascara.lang.markdown.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.SimpleReporter;
import io.github.qishr.cascara.lang.markdown.MarkdownDocument;
import io.github.qishr.cascara.lang.markdown.ast.Alignment;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownTableCellNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownTableNode;

public class MarkdownTableTests {

        MarkdownParser parser;

    @BeforeEach
    void init() {
        Reporter reporter = new SimpleReporter().setLevel(Level.TRACE);
        parser = new MarkdownParser();
        parser.setReporter(reporter);
    }

    @Test
    void testSimpleTable() {
        String input = """
                | Header A | Header B |
                |----------|----------|
                | Cell 1   | Cell 2   |
                | Cell 3   | Cell 4   |""";

        MarkdownDocument doc = parser.parse(input);

        assertTrue(doc.getChildren().get(0) instanceof MarkdownTableNode, "First block should be a table");
        MarkdownTableNode table = (MarkdownTableNode) doc.getChildren().get(0);

        assertEquals(3, table.getRows().size()); // 1 Header + 2 Body rows
        assertEquals(2, table.getRows().get(0).getCells().size()); // 2 columns
    }

    @Test
    void testTableWithPipes() {
        // Testing both "contained" pipes and "open" pipes
        String input = """
                | Header 1 | Header 2 |
                | --- | --- |
                | Row 1 | Row 2 |
                Row 3 | Row 4"""; // Last line doesn't have leading/trailing pipes

        MarkdownDocument doc = parser.parse(input);

        MarkdownTableNode table = (MarkdownTableNode) doc.getChildren().get(0);
        assertEquals(3, table.getRows().size(), "Should have header + 2 data rows");

        // Check that Row 3 | Row 4 still parsed as 2 cells
        assertEquals(2, table.getRows().get(2).getCells().size());
    }

@Test
void testTableAlignment() {
    String input = """
            | Left | Center | Right |
            |:--- | :---: | ---:|
            | 1 | 2 | 3 |""";

    MarkdownDocument doc = parser.parse(input);
    MarkdownTableNode table = (MarkdownTableNode) doc.getChildren().get(0);

    List<MarkdownTableCellNode> headerCells = table.getRows().get(0).getCells();
    assertEquals(Alignment.LEFT, headerCells.get(0).getAlignment());
    assertEquals(Alignment.CENTER, headerCells.get(1).getAlignment());
    assertEquals(Alignment.RIGHT, headerCells.get(2).getAlignment());
}
}
