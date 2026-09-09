package net.teujaem.usershop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * 판매자가 오프라인일 때 쌓이는 정산 대기금 1건(구매 1회) = 1행.
 * 그냥 새 행을 추가(insert)하기만 하면 되므로 read-modify-write 경쟁 상태가 없습니다.
 * sellerUuid로 조회 후 합산하고, 지급 시 그 행들을 한 트랜잭션에서 삭제합니다.
 */
@Entity
@Table(name = "usershop_pending_earnings")
public class PendingEarningEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID sellerUuid;
    private String buyerName;
    private int amount;

    protected PendingEarningEntity() {
        // JPA 전용
    }

    public PendingEarningEntity(UUID sellerUuid, String buyerName, int amount) {
        this.sellerUuid = sellerUuid;
        this.buyerName = buyerName;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public UUID getSellerUuid() { return sellerUuid; }
    public String getBuyerName() { return buyerName; }
    public int getAmount() { return amount; }
}