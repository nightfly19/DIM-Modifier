package com.github.cfogrady.dim.modifier.undo;

public interface Command {
    void execute();

    void undo();
}
