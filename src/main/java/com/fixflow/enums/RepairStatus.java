package com.fixflow.enums;

public enum RepairStatus {
    RECEIVED("Received"),
    DIAGNOSING("Diagnosing"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    COLLECTED("Collected"),
    CANCELLED("Cancelled");

    private final String displayName;

    RepairStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isTerminal() {
        return this == COLLECTED || this == CANCELLED;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
