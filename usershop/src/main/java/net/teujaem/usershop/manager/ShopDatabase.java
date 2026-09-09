package net.teujaem.usershop.manager;

import jakarta.persistence.EntityManager;
import net.teujaem.jpalib.jpa.JpaManager;
import net.teujaem.spFramework.SPFramework;
import net.teujaem.usershop.entity.PendingEarningEntity;
import net.teujaem.usershop.entity.ReturnedItemEntity;
import net.teujaem.usershop.entity.ShopEntryEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * SP-Framework의 JpaManager(= DataBase가 내부적으로 감싸고 있는 것과 같은 것)를
 * DatabaseController.createJpaManager(...)로 직접 받아서 사용합니다.
 *
 * DataBase 래퍼는 find(단건)/save/delete만 제공해 "전체 목록 조회"가 안 되지만,
 * JpaManager.execute/executeAsync(Function<EntityManager,T>)를 쓰면 JPQL로
 * 직접 조회하거나, 조회+삭제를 하나의 트랜잭션으로 묶어 원자적으로 처리할 수 있습니다.
 * (JpaManager.execute는 내부적으로 트랜잭션을 begin → 콜백 실행 → commit 하고,
 *  예외 시 rollback 합니다 - 그래서 아래 메서드들은 read-modify-write 경쟁 상태가 없습니다.)
 *
 * 주의: executeAsync의 콜백/후속(CompletableFuture 체이닝)은 SP-Framework 내부 DB
 * 스레드풀에서 실행됩니다(메인 스레드 아님). Bukkit API를 다루는 후속 처리는
 * 반드시 Bukkit.getScheduler().runTask(plugin, ...)로 메인 스레드로 옮기세요.
 */
public class ShopDatabase {

    private final JpaManager jpaManager;

    public ShopDatabase() {
        this.jpaManager = SPFramework.getInstance().getDatabaseController().createJpaManager(
                ShopEntryEntity.class,
                ReturnedItemEntity.class,
                PendingEarningEntity.class
        );
    }

    // ---------- 판매글(ShopEntryEntity) ----------

    /** 활성 판매글 전체를 JPQL로 한 번에 조회합니다. (DataBase에는 없는 findAll을 직접 구현) */
    public CompletableFuture<List<ShopEntryEntity>> findAllEntries() {
        return jpaManager.executeAsync(em ->
                em.createQuery("SELECT e FROM ShopEntryEntity e", ShopEntryEntity.class).getResultList());
    }

    public CompletableFuture<ShopEntryEntity> findEntry(UUID id) {
        return jpaManager.executeAsync(em -> em.find(ShopEntryEntity.class, id));
    }

    public CompletableFuture<Void> saveEntry(ShopEntryEntity entity) {
        return jpaManager.executeAsync(em -> {
            em.merge(entity);
            return null;
        });
    }

    public CompletableFuture<Void> deleteEntry(UUID id) {
        return jpaManager.executeAsync(em -> {
            ShopEntryEntity found = em.find(ShopEntryEntity.class, id);
            if (found != null) em.remove(found);
            return null;
        });
    }

    /** 모든 판매글을 한 번에 지웁니다 (관리자 초기화). */
    public CompletableFuture<Void> deleteAllEntries() {
        return jpaManager.executeAsync(em -> {
            em.createQuery("DELETE FROM ShopEntryEntity").executeUpdate();
            return null;
        });
    }

    // ---------- 반환된 아이템(ReturnedItemEntity) : 판매자 uuid로 조회 ----------

    public CompletableFuture<List<String>> findReturnedBase64(UUID ownerUuid) {
        return jpaManager.executeAsync(em -> em.createQuery(
                        "SELECT r.itemBase64 FROM ReturnedItemEntity r WHERE r.ownerUuid = :owner ORDER BY r.id",
                        String.class)
                .setParameter("owner", ownerUuid)
                .getResultList());
    }

    public CompletableFuture<Void> addReturnedItem(UUID ownerUuid, String itemBase64) {
        return jpaManager.executeAsync(em -> {
            em.persist(new ReturnedItemEntity(ownerUuid, itemBase64));
            return null;
        });
    }

    /**
     * oneBasedIndex 위치의 아이템을 조회와 동시에(한 트랜잭션 안에서) 삭제하고 base64를 반환합니다.
     * 조회-메모리수정-저장 3단계가 아니라 DB 트랜잭션 하나로 처리되어 경쟁 상태가 없습니다.
     */
    public CompletableFuture<String> takeReturnedItemAt(UUID ownerUuid, int oneBasedIndex) {
        return jpaManager.executeAsync(em -> {
            List<ReturnedItemEntity> rows = em.createQuery(
                            "SELECT r FROM ReturnedItemEntity r WHERE r.ownerUuid = :owner ORDER BY r.id",
                            ReturnedItemEntity.class)
                    .setParameter("owner", ownerUuid)
                    .getResultList();
            if (oneBasedIndex < 1 || oneBasedIndex > rows.size()) {
                return null;
            }
            ReturnedItemEntity target = rows.get(oneBasedIndex - 1);
            String base64 = target.getItemBase64();
            em.remove(target);
            return base64;
        });
    }

    // ---------- 정산 대기금(PendingEarningEntity) : 판매자 uuid로 조회, 새 행 추가만 하면 됨 ----------

    /** 정산 대기금을 새 행으로 추가합니다. 단순 insert라 경쟁 상태가 없습니다. */
    public CompletableFuture<Void> addPending(UUID sellerUuid, String buyerName, int amount) {
        return jpaManager.executeAsync(em -> {
            em.persist(new PendingEarningEntity(sellerUuid, buyerName, amount));
            return null;
        });
    }

    /**
     * 판매자의 정산 대기금을 전부 조회해서 "구매자 이름 -> 합계" 로 합산하고,
     * 조회에 사용된 행들을 같은 트랜잭션 안에서 삭제합니다(조회와 삭제가 원자적).
     */
    public CompletableFuture<Map<String, Integer>> takePending(UUID sellerUuid) {
        return jpaManager.executeAsync(em -> {
            List<PendingEarningEntity> rows = em.createQuery(
                            "SELECT p FROM PendingEarningEntity p WHERE p.sellerUuid = :seller",
                            PendingEarningEntity.class)
                    .setParameter("seller", sellerUuid)
                    .getResultList();
            Map<String, Integer> result = new LinkedHashMap<>();
            for (PendingEarningEntity row : rows) {
                result.merge(row.getBuyerName(), row.getAmount(), Integer::sum);
                em.remove(row);
            }
            return result;
        });
    }
}