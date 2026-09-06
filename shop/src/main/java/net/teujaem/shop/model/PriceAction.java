package net.teujaem.shop.model;

public enum PriceAction {
    DEFAULT("기본"),
    NOW("현재"),
    MIN("최소"),
    MAX("최대");

    private final String displayName;

    PriceAction(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
