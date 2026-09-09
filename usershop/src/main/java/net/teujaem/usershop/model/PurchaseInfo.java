package net.teujaem.usershop.model;

import java.util.UUID;

/**
 * ProxyData.sendToServer(...) 로 다른 서버에 브로드캐스트되는 구매 정보.
 * Gson으로 JSON 문자열로 직렬화되어 ProxyEvent의 value(String)로 전달됩니다.
 */
public class PurchaseInfo {

    public UUID listingId;
    public UUID buyerUuid;
    public String buyerName;
    public UUID sellerUuid;
    public String sellerName;
    public int price;
    public String itemDisplayName;
    public String serverName;
    public long timestamp;

    public PurchaseInfo() {
    }

    public PurchaseInfo(UUID listingId, UUID buyerUuid, String buyerName,
                         UUID sellerUuid, String sellerName, int price,
                         String itemDisplayName, String serverName) {
        this.listingId = listingId;
        this.buyerUuid = buyerUuid;
        this.buyerName = buyerName;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.price = price;
        this.itemDisplayName = itemDisplayName;
        this.serverName = serverName;
        this.timestamp = System.currentTimeMillis();
    }
}
