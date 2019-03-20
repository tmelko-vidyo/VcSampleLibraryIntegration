package com.vidyo.app.utils;

public class CallStatusEvent {

    private int status;
    private int error;
    private String message;

    public CallStatusEvent(int status, int error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public int getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }
}