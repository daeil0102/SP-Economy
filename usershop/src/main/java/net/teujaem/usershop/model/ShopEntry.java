package net.teujaem.usershop.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * 유저상점에 등록된 판매 물품 1건.
 */
public class ShopEntry {

    private final UUID id;
    private final ItemStack item;
    private final int price;
    private final UUID sellerUuid;
    private final String sellerName;
    private final long expireAt;

    public ShopEntry(UUID id, ItemStack item, int price, UUID sellerUuid, String sellerName, long expireAt) {
        this.id = id;
        this.item = item;
        this.price = price;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.expireAt = expireAt;
    }

    public UUID getId() {
        return id;
    }

    public ItemStack getItem() {
        return item;
    }

    public int getPrice() {
        return price;
    }

    public UUID getSellerUuid() {
        return sellerUuid;
    }

    public String getSellerName() {
        return sellerName;
    }

    public long getExpireAt() {
        return expireAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expireAt;
    }
}
