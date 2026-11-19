package com.atomiclegacy.rtty;

/** Raised when an HBM redstone-over-radio command is malformed. */
public final class RttyFunctionException extends RuntimeException {
    public RttyFunctionException(String message) {
        super(message);
    }
}
