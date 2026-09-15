package com.austin.common.exception;

public class SearchTemporarilyUnavailableException extends RuntimeException {

    public SearchTemporarilyUnavailableException(String message) {
        super(message);
    }

    public SearchTemporarilyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
