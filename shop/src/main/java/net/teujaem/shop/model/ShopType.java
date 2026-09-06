package net.teujaem.shop.model;

/**
 * 상점의 동작 방식을 나타냅니다.
 */
public enum ShopType {
    /** 고정 가격으로 사고 파는 일반 상점 */
    NORMAL("일반"),
    /** 시간에 따라 가격이 변동하는 상점 */
    CHANGE("시세"),
    /** 화폐 대신 아이템으로 교환하는 상점 */
    TRADE("거래");

    private final String displayName;

    ShopType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static ShopType fromDisplayName(String text) {
        for (ShopType type : values()) {
            if (type.displayName.equals(text)) {
                return type;
            }
        }
        return null;
    }
}
