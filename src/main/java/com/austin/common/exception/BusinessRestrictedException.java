package com.austin.common.exception;

public class BusinessRestrictedException extends RuntimeException {

    public BusinessRestrictedException(String message) {
        super(message);
    }
}
