package de.somkat.iceboatracing.race;

public enum RaceState {
    WAITING("Waiting for players"),
    COUNTDOWN("Starting in..."),
    RACING("Racing"),
    ENDING("Race ending"),
    FINISHED("Race finished");

    private final String displayName;

    RaceState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this == COUNTDOWN || this == RACING;
    }

    public boolean canJoin() {
        return this == WAITING;
    }
}