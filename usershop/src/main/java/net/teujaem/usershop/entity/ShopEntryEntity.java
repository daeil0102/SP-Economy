package net.teujaem.usershop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * 유저상점에 등록된 판매글 1건을 나타내는 JPA 엔티티.
 * SP-Framework의 DataBase(공유 MariaDB)에 저장되어, 같은 DB를 바라보는
 * 모든 서버에서 조회 가능합니다.
 */
@Entity
@Table(name = "usershop_entries")
public class ShopEntryEntity {

    @Id
    @Column(updatable = false)
    private UUID id;

    @Lob
    @Column(length = 65535)
    private String itemBase64;

    private int price;

    private UUID sellerUuid;

    private String sellerName;

    private long expireAt;

    protected ShopEntryEntity() {
        // JPA 전용
    }

    public ShopEntryEntity(UUID id, String itemBase64, int price, UUID sellerUuid, String sellerName, long expireAt) {
        this.id = id;
        this.itemBase64 = itemBase64;
        this.price = price;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.expireAt = expireAt;
    }

    public UUID getId() {
        return id;
    }

    public String getItemBase64() {
        return itemBase64;
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
}