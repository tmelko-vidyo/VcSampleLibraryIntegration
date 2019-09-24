package com.vidyo.app.utils;

public class ParticipantsChangeEvent {

    private int actualParticipantsCount;

    public ParticipantsChangeEvent(int actualParticipantsCount) {
        this.actualParticipantsCount = actualParticipantsCount;
    }

    public int getActualParticipantsCount() {
        return actualParticipantsCount;
    }
}