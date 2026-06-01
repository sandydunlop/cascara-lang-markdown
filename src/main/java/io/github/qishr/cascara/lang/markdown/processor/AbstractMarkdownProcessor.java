package io.github.qishr.cascara.lang.markdown.processor;

import io.github.qishr.cascara.common.diagnostic.NoOpReporter;
import io.github.qishr.cascara.common.diagnostic.Reporter;
import io.github.qishr.cascara.common.lang.LanguageOptions;
import io.github.qishr.cascara.common.lang.processor.Processor;
import io.github.qishr.cascara.common.util.ContentType;
import io.github.qishr.cascara.common.util.Properties;
import io.github.qishr.cascara.lang.markdown.MarkdownOptions;

public abstract class AbstractMarkdownProcessor<P extends Processor> implements Processor {
    public static final ContentType MARKDOWN_CONTENT_TYPE = new ContentType("Markdown")
        .withSuffix(".md")
        .withType("text/markdown")
        .withType("text/x-markdown");

    protected MarkdownOptions options = new MarkdownOptions();
    protected Reporter reporter = new NoOpReporter();
    private Properties capabilities;

    protected abstract P self();

    public Properties getCapabilities() {
        if (capabilities == null) {
            capabilities = new Properties();
            capabilities.set("contentType", "text/markdown");
        }
        return capabilities;
    }

    @Override
    public ContentType getContentType() {
        return MARKDOWN_CONTENT_TYPE;
    }

    /// {@inheritDoc}
    @Override
    public P setReporter(Reporter reporter) {
        this.reporter = reporter;
        return self();
    }

    /// {@inheritDoc}
    @Override
    public P setOptions(LanguageOptions<?> options) {
        this.options = (MarkdownOptions) options;
        return self();
    }
}
