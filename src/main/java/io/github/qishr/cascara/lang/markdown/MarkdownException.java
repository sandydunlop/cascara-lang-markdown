package io.github.qishr.cascara.lang.markdown;

import java.net.URI;

import io.github.qishr.cascara.common.lang.exception.ParserException;

public class MarkdownException extends ParserException {

    public MarkdownException(String message, Throwable cause) {
        super(message, cause, UNKNOWN_COORD, UNKNOWN_COORD, null);
    }

    public MarkdownException(String message, int line, int column, URI uri) {
        super(message, line, column, uri);
    }

    public MarkdownException(String message, Throwable cause, int line, int column, URI uri) {
        super(message, cause, line, column, uri);
    }
}
