package io.github.qishr.cascara.lang.markdown.ast;

public class MarkdownListItemNode extends MarkdownSequenceNode {
    private boolean isTask = false;
    private boolean isCompleted = false;

    public boolean isTask() { return isTask; }
    public void setTask(boolean task) { isTask = task; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
}
