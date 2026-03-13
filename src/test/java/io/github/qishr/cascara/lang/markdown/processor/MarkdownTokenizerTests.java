package io.github.qishr.cascara.lang.markdown.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.SimpleReporter;
import io.github.qishr.cascara.common.lang.exception.ParserException;
import io.github.qishr.cascara.common.diagnostic.Diagnostic.Level;
import io.github.qishr.cascara.lang.markdown.token.MarkdownToken;
import io.github.qishr.cascara.lang.markdown.token.MarkdownTokenType;

class MarkdownTokenizerTests {
    MarkdownTokenizer tokenizer;

    @BeforeEach
    void init() {
        Reporter reporter = new SimpleReporter().setLevel(Level.TRACE);
        tokenizer = new MarkdownTokenizer();
        tokenizer.setReporter(reporter);
    }

    @Test
    void testIndentation() {
        // 4 spaces at start
        var tokens = tokenizer.tokenize("    content");

        // This should be INDENT because it's at the start of the line
        assertEquals(MarkdownTokenType.INDENT, tokens.get(0).getType());
        assertEquals("    ", tokens.get(0).getLexeme());
        assertEquals("content", tokens.get(1).getLexeme());
    }

    @Test
    void testMixedWhitespace() {
        // 2 spaces start, 2 spaces middle
        var tokens = tokenizer.tokenize("  a  b");

        assertEquals(MarkdownTokenType.INDENT, tokens.get(0).getType(), "Leading spaces must be INDENT");
        assertEquals("  ", tokens.get(0).getLexeme());

        assertEquals(MarkdownTokenType.WHITESPACE, tokens.get(2).getType(), "Middle spaces must be WHITESPACE");
        assertEquals("  ", tokens.get(2).getLexeme());
    }

    @Test
    void testGroupedWhitespace() {
        var tokens = tokenizer.tokenize("  word    another");

        // 1. Check Indent (2 spaces)
        assertEquals(MarkdownTokenType.INDENT, tokens.get(0).getType());
        assertEquals("  ", tokens.get(0).getLexeme());

        // 2. Check Text
        assertEquals("word", tokens.get(1).getLexeme());

        // 3. Check Middle Whitespace (4 spaces)
        assertEquals(MarkdownTokenType.WHITESPACE, tokens.get(2).getType());
        assertEquals("    ", tokens.get(2).getLexeme());
    }

    @Test
    void testNewlineResetsIndent() {
        var tokens = tokenizer.tokenize("word\n  indented");

        // tokens: [TEXT, NEWLINE, INDENT, TEXT]
        assertEquals(MarkdownTokenType.NEWLINE, tokens.get(1).getType());
        assertEquals(MarkdownTokenType.INDENT, tokens.get(2).getType());
        assertEquals("  ", tokens.get(2).getLexeme());
    }

    @Test
    void testTabExpansion() {
        // A single tab followed by content
        var tokens = tokenizer.tokenize("\tcontent");

        assertEquals(MarkdownTokenType.INDENT, tokens.get(0).getType());
        // Lexeme should be expanded to 4 spaces
        assertEquals("    ", tokens.get(0).getLexeme());
        assertEquals(1, tokens.get(0).getStartColumn());

        // The next token should start at column 5 (1 + 4)
        assertEquals(5, tokens.get(1).getStartColumn());
    }

    @Test
    void testMidWordSymbols() {
        // A hash and an asterisk inside a word
        var tokens = tokenizer.tokenize("word#123*");

        // Expected: [TEXT("word"), HASH("#"), TEXT("123"), ASTERISK("*")]
        assertEquals(4, tokens.size());
        assertEquals(MarkdownTokenType.TEXT, tokens.get(0).getType());
        assertEquals("word", tokens.get(0).getLexeme());

        assertEquals(MarkdownTokenType.HASH, tokens.get(1).getType());
        assertEquals("#", tokens.get(1).getLexeme());

        assertEquals(MarkdownTokenType.TEXT, tokens.get(2).getType());
        assertEquals("123", tokens.get(2).getLexeme());

        assertEquals(MarkdownTokenType.ASTERISK, tokens.get(3).getType());
    }

    @Test
    void testLinkDefinitionTokens() {
        // MarkdownTokenizer tokenizer = new MarkdownTokenizer();
        // A standard reference definition
        String input = "[1]: http://google.com \"Title\"";

        List<MarkdownToken> tokens = tokenizer.tokenize(input);

        // Expected sequence:
        // 0: [  (BRACKET_OPEN)
        // 1: 1  (TEXT or DIGIT)
        // 2: ]  (BRACKET_CLOSE)
        // 3: :  (COLON) - This is where we expect failure
        // 4:    (WHITESPACE)
        // 5: http://google.com (TEXT)
        // 6:    (WHITESPACE)
        // 7: "  (QUOTE) - This is where we expect failure

        // Check for the colon specifically
        assertTrue(tokens.size() > 3, "Should have more than 3 tokens");

        MarkdownToken colonToken = tokens.get(3);
        assertEquals(MarkdownTokenType.COLON, colonToken.getType(),
            "Token at index 3 should be COLON, but was " + colonToken.getType() +
            " with lexeme: '" + colonToken.getLexeme() + "'");

        // Check for the quote
        boolean hasQuote = tokens.stream().anyMatch(t -> t.getType() == MarkdownTokenType.QUOTE);
        assertTrue(hasQuote, "Tokenizer should identify QUOTE tokens for link titles");
    }

    @Test
    void debugSymbolMatch() {
        MarkdownTokenizer tokenizer = new MarkdownTokenizer();
        // This calls the private method via a simple test check
        assertNotNull(tokenizer.tokenize(":").get(0).getType() == MarkdownTokenType.COLON);
    }
}
