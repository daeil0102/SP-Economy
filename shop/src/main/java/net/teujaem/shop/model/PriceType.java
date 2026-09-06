package net.teujaem.shop.model;

public enum PriceType {
    BUY("구매"),
    SELL("판매");

    private final String displayName;

    PriceType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static PriceType fromDisplayName(String text) {
        for (PriceType type : values()) {
            if (type.displayName.equals(text)) {
                return type;
            }
        }
        return null;
    }
}
