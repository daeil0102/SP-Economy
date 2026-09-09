package net.teujaem.usershop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * 만료되어 판매자에게 돌아간 아이템 1개 = 1행.
 * ownerUuid로 JPQL 조회(SELECT ... WHERE ownerUuid = ?)해서 그 판매자의 보관함 전체를 가져옵니다.
 */
@Entity
@Table(name = "usershop_returned_items")
public class ReturnedItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID ownerUuid;

    @Lob
    @Column(length = 65535)
    private String itemBase64;

    protected ReturnedItemEntity() {
        // JPA 전용
    }

    public ReturnedItemEntity(UUID ownerUuid, String itemBase64) {
        this.ownerUuid = ownerUuid;
        this.itemBase64 = itemBase64;
    }

    public Long getId() { return id; }
    public UUID getOwnerUuid() { return ownerUuid; }
    public String getItemBase64() { return itemBase64; }
}