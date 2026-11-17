package com.fixflow.exception;

import com.fixflow.enums.RepairStatus;


public class InvalidStatusTransitionException extends Exception {

    private final RepairStatus fromStatus;
    private final RepairStatus toStatus;

    public InvalidStatusTransitionException(RepairStatus fromStatus, RepairStatus toStatus, String reason) {
        super("Invalid status transition from '" + (fromStatus != null ? fromStatus.getDisplayName() : "None") +
              "' to '" + (toStatus != null ? toStatus.getDisplayName() : "None") + "'. Reason: " + reason);
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    public RepairStatus getFromStatus() {
        return fromStatus;
    }

    public RepairStatus getToStatus() {
        return toStatus;
    }
}

