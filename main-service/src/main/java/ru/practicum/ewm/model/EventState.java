package ru.practicum.ewm.model;

public enum EventState {
    PENDING,
    PUBLISHED,
    CANCELED;

    public static boolean isValidState(String state) {
        if (state == null) return true;
        try {
            EventState.valueOf(state.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}