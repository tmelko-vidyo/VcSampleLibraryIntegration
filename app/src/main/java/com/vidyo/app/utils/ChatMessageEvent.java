package com.vidyo.app.utils;

public class ChatMessageEvent {

    private String name;
    private String message;

    private boolean isGroup;

    /* Private chat message only */
    private String uri;

    public ChatMessageEvent(String name, String message, boolean isGroup, String uri) {
        this.name = name;
        this.message = message;
        this.isGroup = isGroup;
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public String getUri() {
        return uri;
    }
}