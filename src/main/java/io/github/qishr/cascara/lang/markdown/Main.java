package io.github.qishr.cascara.lang.markdown;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.diagnostic.SimpleReporter;
import io.github.qishr.cascara.lang.markdown.processor.MarkdownParser;

public class Main {
    public static void main(String[] args) {
        Main main = new Main();
        main.run();
    }

    public void run() {
        String content = getTextResource("/doc.md");
        Reporter reporter = new SimpleReporter();
        MarkdownParser parser = new MarkdownParser().setReporter(reporter);
        MarkdownDocument doc = parser.parse(content);
        System.out.println(doc.getString());
    }

    public final String getTextResource(String resourcePath) {
        InputStream is = getClass().getResourceAsStream(resourcePath);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            return br.lines().collect(Collectors.joining("\n"));
        } catch (Exception _) {
            return "";
        }
    }
}
