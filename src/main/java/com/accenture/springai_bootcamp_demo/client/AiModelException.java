package com.accenture.springai_bootcamp_demo.client;

/** A local model could not produce a usable answer. */
public class AiModelException extends RuntimeException {
    public AiModelException(String message) {
        super(message);
    }

    public AiModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
