package com.example.food.auth;

public class PasswordLoginLockedException extends IllegalArgumentException {

    private final long remainingSeconds;

    public PasswordLoginLockedException(long remainingSeconds) {
        super("Password login locked; retry after " + remainingSeconds + " seconds");
        this.remainingSeconds = remainingSeconds;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }
}
