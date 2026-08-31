package com.example.food.shopping;

import java.util.Locale;

public enum ShoppingItemStatus {
    PENDING(false),
    PURCHASING(false),
    PURCHASED(false),
    READY(true),
    SKIPPED(false);

    private final boolean ready;

    ShoppingItemStatus(boolean ready) {
        this.ready = ready;
    }

    public boolean isReady() {
        return ready;
    }

    public static ShoppingItemStatus from(String value, Boolean legacyChecked) {
        if (value == null || value.isBlank()) {
            return Boolean.TRUE.equals(legacyChecked) ? READY : PENDING;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("采购状态不合法");
        }
    }
}
