package com.safetransact.safetransact;

public class RequestInProgressException extends RuntimeException {

    public RequestInProgressException(String message) {
        super(message);
    }
}
