package io.github.qishr.cascara.lang.markdown.processor;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.lang.LanguageOptions;
import io.github.qishr.cascara.common.lang.processor.Parser;
import io.github.qishr.cascara.common.util.ContentType;
import io.github.qishr.cascara.lang.markdown.MarkdownDocument;
import io.github.qishr.cascara.lang.markdown.MarkdownOptions;
import io.github.qishr.cascara.lang.markdown.ast.Alignment;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownBlockquoteNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownCodeBlockNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownHeadingNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownListItemNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownListNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownParagraphNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownSequenceNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownTableCellNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownTableNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownTableRowNode;
import io.github.qishr.cascara.lang.markdown.ast.MarkdownThematicBreakNode;
import io.github.qishr.cascara.lang.markdown.token.MarkdownToken;
import io.github.qishr.cascara.lang.markdown.token.MarkdownTokenType;


public class MarkdownParser extends AbstractMarkdownProcessor<MarkdownParser> implements Parser<MarkdownDocument, MarkdownToken> {
    public static final ContentType MARKDOWN_CONTENT_TYPE = new ContentType("Markdown")
        .withSuffix(".md")
        .withType("text/markdown")
        .withType("text/x-markdown");

    private Reporter reporter;
    private MarkdownOptions options;

    private List<MarkdownToken> tokens;
    private int index;
    private MarkdownToken currentToken;

    private final java.util.Map<String, String> references = new java.util.HashMap<>();

    @Override protected MarkdownParser self() { return this; }

    @Override
    public MarkdownDocument parse(String text) {
        return parse(text, null);
    }

    @Override
    public MarkdownDocument parse(String text, URI uri) {
        var tokenizer = new MarkdownTokenizer();
        tokenizer.setOptions(options);
        return parse(tokenizer.tokenize(text, uri), uri);
    }

    @Override
    public MarkdownDocument parse(List<MarkdownToken> tokens) {
        return parse(tokens, null);
    }

    @Override
    public MarkdownDocument parse(List<MarkdownToken> tokens, URI uri) {
        this.tokens = tokens;
        this.index = 0;

        // --- PASS 1: Populate Reference Map ---
        int savedIndex = 0;
        while (index < tokens.size()) {
            sync();
            if (check(MarkdownTokenType.BRACKET_OPEN) && isLinkDefinition()) {
                parseLinkDefinition();
            } else {
                // Skip until the next line to look for more definitions
                while (!isAtEnd() && !check(MarkdownTokenType.NEWLINE)) {
                    consume();
                }
                if (!isAtEnd()) consume(); // Consume the newline
            }
        }

        // --- PASS 2: Actual Block Parsing ---
        this.index = 0; // Reset for the real deal
        MarkdownDocument doc = new MarkdownDocument();
        parseBlocks(this.tokens, doc);

        return doc;
    }

    /// Retrieves a registered link reference by its ID.
    /// Used primarily for testing and inline link resolution.
    public String getReference(String id) {
        if (id == null) return null;
        return references.get(id.trim());
    }



    /**
     * The core dispatcher. Can be called recursively for nested containers.
     */
    private void parseBlocks(List<MarkdownToken> blockTokens, MarkdownSequenceNode parent) {
        trace("parseBlocks");
        // Save state if we are recursing
        List<MarkdownToken> oldTokens = this.tokens;
        int oldIndex = this.index;

        this.tokens = blockTokens;
        this.index = 0;

        while (!isAtEnd()) {
            trace("parseBlocks - loop");
            sync();

            // 1. Skip blank lines so we are always looking at the START of a potential block
            if (check(MarkdownTokenType.NEWLINE)) {
                consume();
                continue;
            }

            // 1. Check for Link Definition
            if (check(MarkdownTokenType.BRACKET_OPEN) && isLinkDefinition()) {
                parseLinkDefinition();
            }
            // 2. Check for Blockquote/Heading
            else if (check(MarkdownTokenType.GT)) {
                parseBlockquote(parent);
            } else if (check(MarkdownTokenType.HASH)) {
                parseHeading(parent);
            }
            // 3. PRIORITY: Check for Thematic Break BEFORE List
            else if ((check(MarkdownTokenType.ASTERISK) ||
                      check(MarkdownTokenType.DASH) ||
                      check(MarkdownTokenType.UNDERSCORE)) && isThematicBreak()) {
                parseThematicBreak(parent);
            }
            // 4. Now safe to check for Lists - ONLY if followed by whitespace
            else if (isAtListStart()) {
                parseList(parent);
            }
            // 5. Fenced Code Block
            else if (check(MarkdownTokenType.BACKTICK) && isFencedStart()) {
                parseFencedCodeBlock(parent);
            }
            // 6. Indented Code Block (Check after other blocks like lists)
            else if (check(MarkdownTokenType.INDENT)) {
                parseIndentedCodeBlock(parent);
            }
            else if (check(MarkdownTokenType.NEWLINE)) {
                consume();
            }
            else {
                parseParagraph(parent);
            }
        }

        // Restore state
        this.tokens = oldTokens;
        this.index = oldIndex;
        sync();
    }

    private void parseBlockquote(MarkdownSequenceNode parent) {
        trace("parseBlockquote");
        MarkdownBlockquoteNode quote = new MarkdownBlockquoteNode();
        List<MarkdownToken> quoteTokens = new ArrayList<>();

        while (!isAtEnd()) {
            if (check(MarkdownTokenType.GT)) {
                consume(); // Eat '>'

                // Optional space after '>'
                if (check(MarkdownTokenType.WHITESPACE)) {
                    consume();
                }

                // Collect tokens until the end of the line
                while (!isAtEnd() && !check(MarkdownTokenType.NEWLINE)) {
                    quoteTokens.add(consume());
                }

                // Eat the newline and continue to see if the next line is also a quote
                if (check(MarkdownTokenType.NEWLINE)) {
                    quoteTokens.add(consume());
                }
            } else {
                // If the next line doesn't start with '>', the blockquote is over
                break;
            }
        }

        // Recursively parse the collected tokens as blocks inside the quote
        parseBlocks(quoteTokens, quote);
        parent.add(quote);
    }

    private void parseHeading(MarkdownSequenceNode parent) {
        trace("parseHeading");
        int level = 0;
        while (check(MarkdownTokenType.HASH)) {
            level++;
            consume();
        }

        List<MarkdownToken> inlineTokens = new ArrayList<>();
        while (!isAtEnd() && !check(MarkdownTokenType.NEWLINE)) {
            inlineTokens.add(consume());
        }

        MarkdownHeadingNode heading = new MarkdownHeadingNode(Math.min(level, 6));
        heading.getChildren().addAll(new InlineParser(reporter, references).parse(inlineTokens));
        parent.add(heading);
    }

    private void parseParagraph(MarkdownSequenceNode parent) {
        trace("parseParagraph/SetextCheck");
        List<MarkdownToken> allInlineTokens = new ArrayList<>();

        // 1. Collect first line safely
        List<MarkdownToken> firstLine = new ArrayList<>();
        while (!isAtEnd() && !check(MarkdownTokenType.NEWLINE)) {
            firstLine.add(consume());
        }

        if (check(MarkdownTokenType.NEWLINE) && index + 1 < tokens.size()) {
            int savedIndex = index;
            consume(); // skip \n
            if (isTableDelimiter()) {
                index = savedIndex; // Back up
                parseTable(parent, firstLine); // Pass the first line as the header
                return;
            }
            index = savedIndex; // Reset if not a table
        }

        // 2. Peek for Setext Underline
        if (!isAtEnd() && check(MarkdownTokenType.NEWLINE) && index + 1 < tokens.size()) {
            int nextLineStart = index + 1;
            while (nextLineStart < tokens.size() && tokens.get(nextLineStart).getType() == MarkdownTokenType.WHITESPACE) {
                nextLineStart++;
            }

            if (nextLineStart < tokens.size()) {
                MarkdownTokenType nextType = tokens.get(nextLineStart).getType();
                if ((nextType == MarkdownTokenType.EQUALS || nextType == MarkdownTokenType.DASH)
                    && isSetextUnderline(nextLineStart, nextType)) {

                    consume(); // Consume the NEWLINE
                    // Consume the underline tokens
                    while (!isAtEnd() && !check(MarkdownTokenType.NEWLINE)) consume();
                    if (!isAtEnd() && check(MarkdownTokenType.NEWLINE)) consume();

                    MarkdownHeadingNode heading = new MarkdownHeadingNode(nextType == MarkdownTokenType.EQUALS ? 1 : 2);
                    heading.getChildren().addAll(new InlineParser(reporter, references).parse(firstLine));
                    parent.add(heading);
                    return;
                }
            }
        }

        // 3. Continuity: Keep gathering lines
        allInlineTokens.addAll(firstLine);
        while (!isAtEnd()) {
            if (check(MarkdownTokenType.NEWLINE)) {
                // Stop if blank line OR next line is a block start
                if (index + 1 >= tokens.size() ||
                    tokens.get(index + 1).getType() == MarkdownTokenType.NEWLINE ||
                    isAtBlockStart(index + 1)) {
                    break;
                }
                // If it's just a normal line wrapping, treat newline as a space/separator
                allInlineTokens.add(consume());
            }

            // Final safety check before consuming the actual text token
            if (!isAtEnd()) {
                allInlineTokens.add(consume());
            }
        }

        // Final cleanup: if we are sitting on a newline, eat it so the loop can continue
        if (!isAtEnd() && check(MarkdownTokenType.NEWLINE)) consume();

        MarkdownParagraphNode paragraph = new MarkdownParagraphNode();
        paragraph.getChildren().addAll(new InlineParser(reporter, references).parse(allInlineTokens));
        parent.add(paragraph);
    }

    // private void parseTable(MarkdownSequenceNode parent, List<MarkdownToken> headerTokens) {
    //     trace("parseTable");
    //     MarkdownTableNode table = new MarkdownTableNode();

    //     // 1. Parse Header Row
    //     table.addRow(parseRow(headerTokens, true));

    //     // 2. Consume Delimiter Line
    //     if (check(MarkdownTokenType.NEWLINE)) consume();
    //     while (!isAtEnd() && !check(MarkdownTokenType.NEWLINE)) consume();
    //     if (check(MarkdownTokenType.NEWLINE)) consume();

    //     // 3. Parse Body Rows
    //     while (!isAtEnd() && !isAtBlankLine() && !isAtBlockStart(index)) {
    //         List<MarkdownToken> rowTokens = new ArrayList<>();
    //         while (!isAtEnd() && !check(MarkdownTokenType.NEWLINE)) {
    //             rowTokens.add(consume());
    //         }
    //         table.addRow(parseRow(rowTokens, false));
    //         if (check(MarkdownTokenType.NEWLINE)) consume();
    //     }

    //     parent.add(table);
    // }

    private void parseTable(MarkdownSequenceNode parent, List<MarkdownToken> headerLine) {
        MarkdownTableNode table = new MarkdownTableNode();

        // 1. We're currently AT the newline after the header.
        // Consume newline and collect delimiter tokens.
        consume();
        List<MarkdownToken> delimiterTokens = new ArrayList<>();
        while (!isAtEnd() && !check(MarkdownTokenType.NEWLINE)) {
            delimiterTokens.add(consume());
        }

        // 2. Determine alignments for the whole table
        List<Alignment> alignments = parseTableAlignments(delimiterTokens);

        // 3. Add the header row (using the alignments)
        table.addRow(parseRow(headerLine, true, alignments));

        if (check(MarkdownTokenType.NEWLINE)) consume();

        // 4. Parse Body Rows
        while (!isAtEnd() && !isAtBlankLine() && !isAtBlockStart(index)) {
            List<MarkdownToken> rowTokens = new ArrayList<>();
            while (!isAtEnd() && !check(MarkdownTokenType.NEWLINE)) {
                rowTokens.add(consume());
            }
            table.addRow(parseRow(rowTokens, false, alignments));
            if (check(MarkdownTokenType.NEWLINE)) consume();
        }

        parent.add(table);
    }

    private List<Alignment> parseTableAlignments(List<MarkdownToken> delimiterRow) {
        List<Alignment> alignments = new ArrayList<>();
        List<MarkdownToken> cellBuffer = new ArrayList<>();

        for (int i = 0; i < delimiterRow.size(); i++) {
            MarkdownToken t = delimiterRow.get(i);

            if (t.getType() == MarkdownTokenType.PIPE) {
                // Only process the buffer if it's not the very first pipe in the line
                if (!cellBuffer.isEmpty() || i > 0) {
                    alignments.add(calculateAlignment(cellBuffer));
                    cellBuffer.clear();
                }
            } else {
                cellBuffer.add(t);
            }
        }

        // Catch the last column if it wasn't followed by a pipe
        if (!cellBuffer.isEmpty()) {
            alignments.add(calculateAlignment(cellBuffer));
        }

        return alignments;
    }

    private Alignment calculateAlignment(List<MarkdownToken> tokens) {
        boolean startsWithColon = false;
        boolean endsWithColon = false;

        // Filter out whitespace to find the real start/end characters
        List<MarkdownToken> content = tokens.stream()
                .filter(t -> t.getType() != MarkdownTokenType.WHITESPACE)
                .toList();

        if (!content.isEmpty()) {
            startsWithColon = content.get(0).getType() == MarkdownTokenType.COLON;
            endsWithColon = content.get(content.size() - 1).getType() == MarkdownTokenType.COLON;
        }

        if (startsWithColon && endsWithColon) return Alignment.CENTER;
        if (endsWithColon) return Alignment.RIGHT;
        return Alignment.LEFT; // Default alignment
    }

    private MarkdownTableRowNode parseRow(List<MarkdownToken> rowTokens, boolean isHeader, List<Alignment> alignments) {
        MarkdownTableRowNode row = new MarkdownTableRowNode();
        List<MarkdownToken> cellBuffer = new ArrayList<>();
        int cellIndex = 0; // The missing variable

        for (int i = 0; i < rowTokens.size(); i++) {
            MarkdownToken t = rowTokens.get(i);

            if (t.getType() == MarkdownTokenType.PIPE) {
                // Only add a cell if there's content, or if it's not the very first pipe
                if (!cellBuffer.isEmpty() || i > 0) {
                    Alignment align = (cellIndex < alignments.size()) ? alignments.get(cellIndex) : Alignment.LEFT;
                    row.getCells().add(createCell(cellBuffer, isHeader, align));
                    cellBuffer.clear();
                    cellIndex++; // Increment for the next column
                }
            } else {
                cellBuffer.add(t);
            }
        }

        // Add the final cell if there are tokens left after the last pipe
        if (!cellBuffer.isEmpty()) {
            Alignment align = (cellIndex < alignments.size()) ? alignments.get(cellIndex) : Alignment.LEFT;
            row.getCells().add(createCell(cellBuffer, isHeader, align));
        }

        return row;
    }

    // private MarkdownTableCellNode createCell(List<MarkdownToken> cellTokens, boolean isHeader) {
    //     // 1. Trim leading/trailing whitespace tokens for the cell
    //     int start = 0;
    //     while (start < cellTokens.size() && cellTokens.get(start).getType() == MarkdownTokenType.WHITESPACE) {
    //         start++;
    //     }
    //     int end = cellTokens.size();
    //     while (end > start && cellTokens.get(end - 1).getType() == MarkdownTokenType.WHITESPACE) {
    //         end--;
    //     }

    //     List<MarkdownToken> trimmed = cellTokens.subList(start, end);

    //     // 2. Create node and parse inlines
    //     MarkdownTableCellNode cell = new MarkdownTableCellNode(isHeader);
    //     cell.getChildren().addAll(new InlineParser(reporter, references).parse(trimmed));
    //     return cell;
    // }
    private MarkdownTableCellNode createCell(List<MarkdownToken> cellTokens, boolean isHeader, Alignment alignment) {
        // 1. Trim leading/trailing whitespace
        int start = 0;
        while (start < cellTokens.size() && cellTokens.get(start).getType() == MarkdownTokenType.WHITESPACE) {
            start++;
        }
        int end = cellTokens.size();
        while (end > start && cellTokens.get(end - 1).getType() == MarkdownTokenType.WHITESPACE) {
            end--;
        }

        List<MarkdownToken> trimmed = cellTokens.subList(start, end);

        // 2. Create node and set properties
        MarkdownTableCellNode cell = new MarkdownTableCellNode(isHeader);
        cell.setAlignment(alignment);

        // 3. Parse inlines (bold, italic, etc.)
        cell.getChildren().addAll(new InlineParser(reporter, references).parse(trimmed));
        return cell;
    }

    private void parseList(MarkdownSequenceNode parent) {
        trace("parseList");
        // Peek at current token to decide if ordered or unordered
        boolean isOrdered = check(MarkdownTokenType.LIST_NUMBER);
        MarkdownListNode listNode = new MarkdownListNode(isOrdered);

        // Keep consuming markers that belong to this list
        while (!isAtEnd() && isListMarker(currentToken.getType())) {
            parseListItem(listNode);
        }

        parent.add(listNode);
    }

    private void parseListItem(MarkdownListNode list) {
        trace("parseListItem");
        MarkdownListItemNode item = new MarkdownListItemNode();
        consume(); // Consume '*', '-', or '1.'

        if (check(MarkdownTokenType.WHITESPACE)) consume();

        // --- Task List Detection ---
        // We look for: [WHITESPACE/TEXT(x)] ] WHITESPACE
        if (check(MarkdownTokenType.BRACKET_OPEN)) {
            if (isTaskPattern()) {
                item.setTask(true);
                consume(); // [
                MarkdownToken mid = consume(); // ' ' or 'x'
                item.setCompleted(mid.getLexeme().equalsIgnoreCase("x"));
                consume(); // ]
                if (check(MarkdownTokenType.WHITESPACE)) consume();
            }
        }

        List<MarkdownToken> lineTokens = new ArrayList<>();
        while (!isAtEnd() && !check(MarkdownTokenType.NEWLINE)) {
            lineTokens.add(consume());
        }
        if (check(MarkdownTokenType.NEWLINE)) consume();

        // INSTEAD of parseBlocks (which looks for thematic breaks again)
        // Use InlineParser directly for simple list items
        // 1. Create the paragraph node
        MarkdownParagraphNode p = new MarkdownParagraphNode();

        // 2. Add the parsed inline nodes to the paragraph
        p.getChildren().addAll(new InlineParser(reporter, references).parse(lineTokens));

        // 3. Add the paragraph to the list item's content
        item.add(p);

        // Handle nested lists...
        if (check(MarkdownTokenType.INDENT) && isFollowedByListMarker()) {
            consume();
            parseList(item);
        }

        list.add(item);
    }

    void parseLinkDefinition() {
        trace("parseLinkDefinition");
        consume(); // [

        // 1. Capture ID
        StringBuilder idBuilder = new StringBuilder();
        while (!isAtEnd() && !check(MarkdownTokenType.BRACKET_CLOSE)) {
            idBuilder.append(consume().getLexeme());
        }
        consume(); // ]
        consume(); // :

        // Skip optional whitespace after colon
        if (check(MarkdownTokenType.WHITESPACE)) consume();

        // 2. Capture URL (up to whitespace or newline)
        StringBuilder urlBuilder = new StringBuilder();
        while (!isAtEnd() &&
               !check(MarkdownTokenType.WHITESPACE) &&
               !check(MarkdownTokenType.NEWLINE)) {
            urlBuilder.append(consume().getLexeme());
        }

        // 3. Capture Optional Title: "title", 'title', or (title)
        if (!isAtEnd() && check(MarkdownTokenType.WHITESPACE)) {
            consume(); // skip the space

            if (!isAtEnd()) {
                MarkdownTokenType quoteType = peek().getType();
                if (quoteType == MarkdownTokenType.QUOTE ||
                    quoteType == MarkdownTokenType.SINGLE_QUOTE ||
                    quoteType == MarkdownTokenType.PAREN_OPEN) {

                    MarkdownTokenType closingType = (quoteType == MarkdownTokenType.PAREN_OPEN)
                        ? MarkdownTokenType.PAREN_CLOSE
                        : quoteType;

                    consume(); // Open quote/paren
                    StringBuilder titleBuilder = new StringBuilder();
                    while (!isAtEnd() && !check(closingType)) {
                        titleBuilder.append(consume().getLexeme());
                    }
                    if (!isAtEnd()) consume(); // Close quote/paren
                }
            }
        }

        // 4. Cleanup: Consume the trailing newline so it doesn't trigger a new paragraph
        while (!isAtEnd() && !check(MarkdownTokenType.NEWLINE)) {
            consume(); // Eat any trailing garbage on the line
        }
        if (check(MarkdownTokenType.NEWLINE)) consume();

        // Store the mapping
        references.put(idBuilder.toString().trim(), urlBuilder.toString().trim());
    }



    private void parseThematicBreak(MarkdownSequenceNode parent) {
        trace("parseThematicBreak");
        // Consume the rest of the line
        while (!isAtEnd() && !check(MarkdownTokenType.NEWLINE)) {
            consume();
        }
        if (check(MarkdownTokenType.NEWLINE)) consume();

        parent.add(new MarkdownThematicBreakNode());
    }

    private void parseIndentedCodeBlock(MarkdownSequenceNode parent) {
        trace("parseIndentedCodeBlock");
        StringBuilder sb = new StringBuilder();

        while (!isAtEnd()) {
            if (check(MarkdownTokenType.INDENT)) {
                consume(); // Eat the 4 spaces
                while (!isAtEnd() && !check(MarkdownTokenType.NEWLINE)) {
                    sb.append(consume().getLexeme());
                }
                if (check(MarkdownTokenType.NEWLINE)) {
                    sb.append(consume().getLexeme());
                }
            } else if (check(MarkdownTokenType.NEWLINE)) {
                // Peek ahead: is the line after this newline indented?
                if (index + 1 < tokens.size() && tokens.get(index + 1).getType() == MarkdownTokenType.INDENT) {
                    sb.append(consume().getLexeme()); // Keep the blank line
                } else {
                    break; // End of indented block
                }
            } else {
                break;
            }
        }
        parent.add(new MarkdownCodeBlockNode(sb.toString().stripTrailing(), null));
    }

    private void parseFencedCodeBlock(MarkdownSequenceNode parent) {
        trace("parseFencedCodeBlock");
        int openFenceSize = 0;
        while (check(MarkdownTokenType.BACKTICK)) {
            openFenceSize++;
            consume();
        }

        // Info string (language)
        StringBuilder lang = new StringBuilder();
        while (!isAtEnd() && !check(MarkdownTokenType.NEWLINE)) {
            lang.append(consume().getLexeme());
        }
        if (check(MarkdownTokenType.NEWLINE)) consume();

        StringBuilder content = new StringBuilder();
        while (!isAtEnd()) {
            if (check(MarkdownTokenType.BACKTICK) && isClosingFence(openFenceSize)) {
                while (check(MarkdownTokenType.BACKTICK)) consume();
                if (check(MarkdownTokenType.NEWLINE)) consume();
                break;
            }
            content.append(consume().getLexeme());
        }

        parent.add(new MarkdownCodeBlockNode(content.toString(), lang.toString().trim()));
    }

    //
    // Helper Methods
    //

    private boolean isAtBlankLine() {
        if (isAtEnd()) return true;
        if (tokens.get(index).getType() == MarkdownTokenType.NEWLINE) {
            // If it's a newline followed by another newline, it's a blank line
            return (index + 1 >= tokens.size() || tokens.get(index + 1).getType() == MarkdownTokenType.NEWLINE);
        }
        return false;
    }

    // private boolean isTableDelimiter() {
    //     int i = index;
    //     // Tables are usually identified by the SECOND line.
    //     // This helper checks if the current line at 'index' is a delimiter.
    //     boolean hasDash = false;

    //     while (i < tokens.size()) {
    //         MarkdownTokenType type = tokens.get(i).getType();
    //         if (type == MarkdownTokenType.NEWLINE) break;

    //         if (type == MarkdownTokenType.DASH) {
    //             hasDash = true;
    //         } else if (type != MarkdownTokenType.PIPE &&
    //                    type != MarkdownTokenType.COLON &&
    //                    type != MarkdownTokenType.WHITESPACE) {
    //             return false; // Any other char (text, etc) means it's not a delimiter
    //         }
    //         i++;
    //     }
    //     return hasDash;
    // }

    private boolean isTableDelimiter() {
        int i = index;
        boolean hasDash = false;
        boolean hasPipe = false; // Add this!

        while (i < tokens.size()) {
            MarkdownToken t = tokens.get(i);
            MarkdownTokenType type = t.getType();

            if (type == MarkdownTokenType.NEWLINE) break;

            if (type == MarkdownTokenType.DASH) {
                hasDash = true;
            } else if (type == MarkdownTokenType.PIPE) {
                hasPipe = true; // We found a pipe!
            } else if (type == MarkdownTokenType.COLON || type == MarkdownTokenType.WHITESPACE) {
                // Valid
            } else {
                return false;
            }
            i++;
        }

        // To distinguish from a Setext Heading, a table delimiter
        // MUST contain at least one pipe character.
        return hasDash && hasPipe;
    }

    private boolean isTaskPattern() {
        // Need at least: [ , char, ]
        if (index + 2 >= tokens.size()) return false;

        MarkdownToken mid = tokens.get(index + 1);
        MarkdownToken close = tokens.get(index + 2);

        boolean hasValidMid = mid.getType() == MarkdownTokenType.WHITESPACE ||
                             (mid.getType() == MarkdownTokenType.TEXT && mid.getLexeme().equalsIgnoreCase("x"));

        return hasValidMid && close.getType() == MarkdownTokenType.BRACKET_CLOSE;
    }

    private boolean isFencedStart() {
        int i = index;
        int count = 0;
        while (i < tokens.size() && tokens.get(i).getType() == MarkdownTokenType.BACKTICK) {
            count++;
            i++;
        }
        return count >= 3;
    }

    private boolean isClosingFence(int openSize) {
        int i = index;
        int count = 0;
        while (i < tokens.size() && tokens.get(i).getType() == MarkdownTokenType.BACKTICK) {
            count++;
            i++;
        }
        return count >= openSize;
    }

    private boolean isAtBlockStart(int idx) {
        if (idx >= tokens.size()) return false;
        MarkdownToken t = tokens.get(idx);
        MarkdownTokenType type = t.getType();

        // Existing checks
        if (type == MarkdownTokenType.HASH || type == MarkdownTokenType.GT ||
            type == MarkdownTokenType.ASTERISK || type == MarkdownTokenType.DASH ||
            type == MarkdownTokenType.BRACKET_OPEN) return true;

        // NEW: Indented Code check
        if (type == MarkdownTokenType.INDENT) return true;

        // NEW: Fenced Code check (must be at least 3 backticks)
        if (type == MarkdownTokenType.BACKTICK) {
            int count = 0;
            for (int i = idx; i < tokens.size() && tokens.get(i).getType() == MarkdownTokenType.BACKTICK; i++) {
                count++;
            }
            return count >= 3;
        }

        return false;
    }

    private boolean isSetextUnderline(int start, MarkdownTokenType type) {
        int i = start;
        int count = 0;
        while (i < tokens.size()) {
            MarkdownTokenType t = tokens.get(i).getType();
            if (t == MarkdownTokenType.NEWLINE) break;
            if (t == type) {
                count++;
            } else if (t != MarkdownTokenType.WHITESPACE) {
                return false;
            }
            i++;
        }
        return count >= 1; // Even one = or - counts as an underline
    }


    /// Determines if the current tokens represent a link reference definition.
    boolean isLinkDefinition() {
        int i = index;

        if (i >= tokens.size() || tokens.get(i).getType() != MarkdownTokenType.BRACKET_OPEN) return false;

        i++; // Move past [
        while (i < tokens.size() && tokens.get(i).getType() != MarkdownTokenType.BRACKET_CLOSE) {
            if (tokens.get(i).getType() == MarkdownTokenType.NEWLINE) return false;
            i++;
        }

        if (i >= tokens.size() || tokens.get(i).getType() != MarkdownTokenType.BRACKET_CLOSE) return false;
        i++; // Move past ]

        // SKIP WHITESPACE HERE
        while (i < tokens.size() && tokens.get(i).getType() == MarkdownTokenType.WHITESPACE) {
            i++;
        }

        if (i < tokens.size()) {
            MarkdownToken nextToken = tokens.get(i);
            return nextToken.getType() == MarkdownTokenType.COLON || ":".equals(nextToken.getLexeme());
        }

        return false;
    }

    private boolean isAtListStart() {
        if (isAtEnd()) return false;
        MarkdownTokenType type = tokens.get(index).getType();

        // Numbers (1.) are always list starts
        if (type == MarkdownTokenType.LIST_NUMBER) return true;

        // Bullet points (* or -) MUST be followed by whitespace to be a list
        if (type == MarkdownTokenType.ASTERISK || type == MarkdownTokenType.DASH) {
            return lookahead(1) == MarkdownTokenType.WHITESPACE;
        }

        return false;
    }


    private boolean isThematicBreak() {
        int i = index;
        if (i >= tokens.size()) return false;

        MarkdownTokenType type = tokens.get(i).getType();
        // Only *, -, and _ can be thematic breaks
        if (type != MarkdownTokenType.ASTERISK &&
            type != MarkdownTokenType.DASH &&
            type != MarkdownTokenType.UNDERSCORE) return false;

        int count = 0;
        while (i < tokens.size()) {
            MarkdownTokenType t = tokens.get(i).getType();
            if (t == MarkdownTokenType.NEWLINE) break;

            if (t == type) {
                count++;
            } else if (t != MarkdownTokenType.WHITESPACE) {
                // Any non-whitespace, non-matching char means it's a paragraph or list
                return false;
            }
            i++;
        }
        return count >= 3;
    }

    private void sync() {
        if (index < tokens.size()) currentToken = tokens.get(index);
    }

    private MarkdownToken consume() {
        MarkdownToken t = tokens.get(index++);
        sync();
        return t;
    }

    private boolean check(MarkdownTokenType type) {
        return !isAtEnd() && tokens.get(index).getType() == type;
    }

    private MarkdownTokenType lookahead(int offset) {
        if (index + offset >= tokens.size()) return null;
        return (MarkdownTokenType) tokens.get(index + offset).getType();
    }

    private MarkdownToken peek() {
        if (isAtEnd()) return tokens.get(tokens.size() - 1);
        return tokens.get(index);
    }

    private boolean isAtEnd() { return index >= tokens.size(); }

    private boolean isListMarker(MarkdownTokenType type) {
        return type == MarkdownTokenType.ASTERISK ||
               type == MarkdownTokenType.DASH ||
               type == MarkdownTokenType.LIST_NUMBER;
    }

    private boolean isFollowedByListMarker() {
        // Peek at the token after the INDENT
        if (index + 1 >= tokens.size()) return false;
        MarkdownTokenType nextType = tokens.get(index + 1).getType();
        return isListMarker(nextType);
    }

    private void trace(String methodName) {
        if (reporter == null || isAtEnd()) return;
        if (currentToken == null) {
            reporter.trace("I%3d %s: %s",
                    index, methodName, upcomingTokens());
            return;
        }
        reporter.trace("L%3d C%3d I%3d %s: %s",
            currentToken.getStartLine(), currentToken.getStartColumn(), index, methodName, upcomingTokens());
    }

    private String upcomingTokens() {
        return tokens.subList(index, Math.min(index + 3, tokens.size())).stream()
            .map(t -> t.getType().toString()).collect(Collectors.joining(" "));
    }

    @Override public MarkdownParser setReporter(Reporter reporter) { this.reporter = reporter; return this; }
    @Override public MarkdownParser setOptions(LanguageOptions<?> options) { this.options = (MarkdownOptions) options; return this; }
}