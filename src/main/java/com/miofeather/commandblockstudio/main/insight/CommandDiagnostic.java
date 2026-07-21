package com.miofeather.commandblockstudio.main.insight;

public record CommandDiagnostic(int start, int end, String message) {
    public boolean contains(int index) {
        return index >= start && index <= end;
    }
}
