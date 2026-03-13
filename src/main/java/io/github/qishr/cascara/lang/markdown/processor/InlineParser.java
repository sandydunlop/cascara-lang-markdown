package io.github.qishr.cascara.lang.markdown.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.lang.token.Token;
import io.github.qishr.cascara.lang.markdown.ast.*;
import io.github.qishr.cascara.lang.markdown.token.MarkdownToken;
import io.github.qishr.cascara.lang.markdown.token.MarkdownTokenType;

public class InlineParser {
    private final Reporter reporter;
    private final Map<String, String> references;

    public InlineParser(Reporter reporter, Map<String, String> references) {
        this.reporter = reporter;
        this.references = references;
    }

    public List<MarkdownNode> parse(List<MarkdownToken> tokens) {
        List<MarkdownNode> nodes = new ArrayList<>();
        int i = 0;

        while (i < tokens.size()) {
            MarkdownToken currentToken = tokens.get(i);
            MarkdownTokenType type = (MarkdownTokenType) currentToken.getType();
            trace(currentToken, tokens, i, "parse");

            // 1. IMAGES: ![alt](url) OR ![alt][ref]
            if (type == MarkdownTokenType.BANG && lookahead(tokens, i, 1) == MarkdownTokenType.BRACKET_OPEN) {
                int closeB = findClosing(tokens, i + 2, MarkdownTokenType.BRACKET_CLOSE, 1);
                if (closeB != -1) {
                    int next = closeB + 1;
                    while (next < tokens.size() && tokens.get(next).getType() == MarkdownTokenType.WHITESPACE) next++;

                    if (next < tokens.size()) {
                        // Inline Image
                        if (tokens.get(next).getType() == MarkdownTokenType.PAREN_OPEN) {
                            int closeP = findClosing(tokens, next + 1, MarkdownTokenType.PAREN_CLOSE, 1);
                            if (closeP != -1) {
                                String url = aggregateLexemes(tokens.subList(next + 1, closeP));
                                String alt = aggregateLexemes(tokens.subList(i + 2, closeB));
                                nodes.add(new MarkdownImageNode(url, alt));
                                i = closeP + 1;
                                continue;
                            }
                        }
                        // Reference Image
                        else if (tokens.get(next).getType() == MarkdownTokenType.BRACKET_OPEN) {
                            int closeRef = findClosing(tokens, next + 1, MarkdownTokenType.BRACKET_CLOSE, 1);
                            if (closeRef != -1) {
                                String refId = aggregateLexemes(tokens.subList(next + 1, closeRef)).trim();
                                String alt = aggregateLexemes(tokens.subList(i + 2, closeB));
                                String url = references.getOrDefault(refId, "");
                                nodes.add(new MarkdownImageNode(url, alt));
                                i = closeRef + 1;
                                continue;
                            }
                        }
                    }
                }
            }

            // 2. LINKS: [text](url) OR [text][id]
            if (type == MarkdownTokenType.BRACKET_OPEN) {
                int closeB = findClosing(tokens, i + 1, MarkdownTokenType.BRACKET_CLOSE, 1);
                if (closeB != -1) {
                    int next = closeB + 1;
                    while (next < tokens.size() && tokens.get(next).getType() == MarkdownTokenType.WHITESPACE) next++;

                    if (next < tokens.size()) {
                        // Inline
                        if (tokens.get(next).getType() == MarkdownTokenType.PAREN_OPEN) {
                            int closeP = findClosing(tokens, next + 1, MarkdownTokenType.PAREN_CLOSE, 1);
                            if (closeP != -1) {
                                MarkdownLinkNode link = new MarkdownLinkNode(aggregateLexemes(tokens.subList(next + 1, closeP)));
                                link.getChildren().addAll(parse(tokens.subList(i + 1, closeB)));
                                nodes.add(link);
                                i = closeP + 1;
                                continue;
                            }
                        }
                        // Reference
                        if (tokens.get(next).getType() == MarkdownTokenType.BRACKET_OPEN) {
                            int closeRef = findClosing(tokens, next + 1, MarkdownTokenType.BRACKET_CLOSE, 1);
                            if (closeRef != -1) {
                                String refId = aggregateLexemes(tokens.subList(next + 1, closeRef)).trim();
                                if (refId.isEmpty()) refId = aggregateLexemes(tokens.subList(i + 1, closeB)).trim();

                                if (references.containsKey(refId)) {
                                    MarkdownLinkNode link = new MarkdownLinkNode(references.get(refId));
                                    link.getChildren().addAll(parse(tokens.subList(i + 1, closeB)));
                                    nodes.add(link);
                                    i = closeRef + 1;
                                    continue;
                                }
                            }
                        }
                    }
                    // Shortcut
                    String shortcut = aggregateLexemes(tokens.subList(i + 1, closeB)).trim();
                    if (references.containsKey(shortcut)) {
                        MarkdownLinkNode link = new MarkdownLinkNode(references.get(shortcut));
                        link.getChildren().addAll(parse(tokens.subList(i + 1, closeB)));
                        nodes.add(link);
                        i = closeB + 1;
                        continue;
                    }
                }
            }

            // 3. BOLD / 4. EMPHASIS (Consolidated check)
            if (type == MarkdownTokenType.ASTERISK || type == MarkdownTokenType.UNDERSCORE) {
                if (i + 1 < tokens.size() && tokens.get(i + 1).getType() == type) {
                    int closing = findClosing(tokens, i + 2, type, 2);
                    if (closing != -1) {
                        MarkdownBoldNode bold = new MarkdownBoldNode();
                        bold.getChildren().addAll(parse(tokens.subList(i + 2, closing)));
                        nodes.add(bold);
                        i = closing + 2; continue;
                    }
                } else {
                    int closing = findClosing(tokens, i + 1, type, 1);
                    if (closing != -1) {
                        MarkdownEmphasisNode em = new MarkdownEmphasisNode(1);
                        em.getChildren().addAll(parse(tokens.subList(i + 1, closing)));
                        nodes.add(em);
                        i = closing + 1; continue;
                    }
                }
            }

            // 6. CODE SPANS
            if (type == MarkdownTokenType.BACKTICK) {
                int startCount = 0;
                int temp = i;
                while (temp < tokens.size() && tokens.get(temp).getType() == MarkdownTokenType.BACKTICK) {
                    startCount++; temp++;
                }
                int closing = findClosing(tokens, temp, MarkdownTokenType.BACKTICK, startCount);
                if (closing != -1) {
                    String content = aggregateLexemes(tokens.subList(temp, closing));
                    if (content.length() >= 2 && content.startsWith(" ") && content.endsWith(" ") && !content.trim().isEmpty()) {
                        content = content.substring(1, content.length() - 1);
                    }
                    nodes.add(new MarkdownCodeSpanNode(content));
                    i = closing + startCount; continue;
                }
            }

            // 7. AUTOLINKS
            if (type == MarkdownTokenType.LT) {
                int closeAngle = findClosing(tokens, i + 1, MarkdownTokenType.GT, 1);
                if (closeAngle != -1) {
                    String content = aggregateLexemes(tokens.subList(i + 1, closeAngle));
                    if (content.contains("://") || content.contains("@")) {
                        nodes.add(new MarkdownLinkNode(content, content));
                        i = closeAngle + 1; continue;
                    }
                }
            }


            // // 9. HARD LINE BREAKS
            // if (type == MarkdownTokenType.BACKSLASH && lookahead(tokens, i, 1) == MarkdownTokenType.NEWLINE) {
            //     nodes.add(new MarkdownLineBreakNode());
            //     i += 2; // skip \ and \n
            //     continue;
            // }

            // if (type == MarkdownTokenType.WHITESPACE && currentToken.getLexeme().length() >= 2
            //     && lookahead(tokens, i, 1) == MarkdownTokenType.NEWLINE) {
            //     nodes.add(new MarkdownLineBreakNode());
            //     i += 2; // skip spaces and \n
            //     continue;
            // }


            // 9. HARD LINE BREAKS
            // Case 1: Backslash at end of line (Tokenized as ESCAPED_CHAR '\')
            if (type == MarkdownTokenType.ESCAPED_CHAR && "\\".equals(currentToken.getLexeme())
                && lookahead(tokens, i, 1) == MarkdownTokenType.NEWLINE) {
                nodes.add(new MarkdownLineBreakNode());
                i += 2;
                continue;
            }

            // Case 2: Two spaces at end of line
            if (type == MarkdownTokenType.WHITESPACE && currentToken.getLexeme().length() >= 2
                && lookahead(tokens, i, 1) == MarkdownTokenType.NEWLINE) {
                nodes.add(new MarkdownLineBreakNode());
                i += 2;
                continue;
            }



            // 5. FALLBACK: Text merging
            // String lexeme = currentToken.getLexeme();
            // if (!nodes.isEmpty() && nodes.get(nodes.size() - 1) instanceof MarkdownTextNode lastNode) {
            //     nodes.set(nodes.size() - 1, new MarkdownTextNode(lastNode.getText() + lexeme));
            // } else {
            //     nodes.add(new MarkdownTextNode(lexeme));
            // }

            // Inside InlineParser.parse fallback logic
            String lexeme = currentToken.getLexeme();

            // If it's an escaped char like "\*", we just want the "*" part for the text node
            if (type == MarkdownTokenType.ESCAPED_CHAR && lexeme.startsWith("\\") && lexeme.length() > 1) {
                lexeme = lexeme.substring(1);
            }

            if (!nodes.isEmpty() && nodes.get(nodes.size() - 1) instanceof MarkdownTextNode lastNode) {
                nodes.set(nodes.size() - 1, new MarkdownTextNode(lastNode.getText() + lexeme));
            } else {
                nodes.add(new MarkdownTextNode(lexeme));
            }


            i++;
        }
        return nodes;
    }

    private int findClosing(List<MarkdownToken> tokens, int start, MarkdownTokenType type, int count) {
        for (int idx = start; idx <= tokens.size() - count; idx++) {
            boolean match = true;
            for (int j = 0; j < count; j++) {
                if (!tokens.get(idx + j).getType().equals(type)) {
                    match = false; break;
                }
            }
            if (match) return idx;
        }
        return -1;
    }

    private String aggregateLexemes(List<MarkdownToken> tokens) {
        return tokens.stream().map(Token::getLexeme).collect(Collectors.joining());
    }

    private void trace(MarkdownToken t, List<MarkdownToken> tokens, int current, String method) {
        if (reporter == null) return;
        reporter.trace("L%3d C%3d I%3d %s: %s", t.getStartLine(), t.getStartColumn(), current, method, upcoming(tokens, current));
    }

    private String upcoming(List<MarkdownToken> tokens, int current) {
        return tokens.subList(current, Math.min(tokens.size(), current + 3))
                     .stream().map(t -> t.getType().toString()).collect(Collectors.joining(" "));
    }

    private MarkdownTokenType lookahead(List<MarkdownToken> tokens, int currentIdx, int offset) {
        int target = currentIdx + offset;
        if (target < 0 || target >= tokens.size()) return null;
        return (MarkdownTokenType) tokens.get(target).getType();
    }
}