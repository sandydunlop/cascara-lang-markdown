module cascara.lang.markdown {
    requires transitive cascara.common;

    exports io.github.qishr.cascara.lang.markdown;
    exports io.github.qishr.cascara.lang.markdown.ast;
    exports io.github.qishr.cascara.lang.markdown.exception;
    exports io.github.qishr.cascara.lang.markdown.processor;
    exports io.github.qishr.cascara.lang.markdown.token;

    opens io.github.qishr.cascara.lang.markdown;
    opens io.github.qishr.cascara.lang.markdown.ast;
    opens io.github.qishr.cascara.lang.markdown.processor;
    opens io.github.qishr.cascara.lang.markdown.token;
}
