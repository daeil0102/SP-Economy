package net.teujaem.usershop.manager;

import net.teujaem.usershop.entity.ShopEntryEntity;
import net.teujaem.usershop.model.ShopEntry;
import net.teujaem.usershop.util.ItemSerialization;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 유저상점 판매글을 관리합니다.
 *
 * - 활성 판매글 목록은 이 서버의 메모리(entries, 읽기 캐시)에 들고 있어 GUI를 즉시 그릴 수 있게 하고,
 *   실제 저장은 공유 DB(ShopDatabase, MariaDB)에 합니다.
 * - 다른 서버에서 생긴 변경(등록/구매/만료/초기화)은 ProxyEvent를 통해 전달되며,
 *   applyRemoteAdd / applyRemoteRemove / clearCache 로 캐시에만 반영합니다
 *   (다시 DB에 쓰지 않음 - 이미 원본 서버가 저장했기 때문).
 * - 반환된 아이템(보관함) / 정산 대기금은 판매자 UUID로 직접 조회 가능해 굳이 캐시하지 않고,
 *   필요할 때마다 ShopDatabase를 통해 DB에서 바로 읽고 씁니다.
 */
public class ShopManager {

    private final JavaPlugin plugin;
    private final ShopDatabase shopDatabase;

    private final List<ShopEntry> entries = new CopyOnWriteArrayList<>();
    private volatile boolean ready = false;

    public ShopManager(JavaPlugin plugin, ShopDatabase shopDatabase) {
        this.plugin = plugin;
        this.shopDatabase = shopDatabase;
    }

    public boolean isReady() {
        return ready;
    }

    // ---------- 조회 (캐시) ----------

    public List<ShopEntry> getEntries() {
        return entries;
    }

    public ShopEntry getByIndex(int oneBasedIndex) {
        if (oneBasedIndex < 1 || oneBasedIndex > entries.size()) return null;
        return entries.get(oneBasedIndex - 1);
    }

    public ShopEntry getById(UUID id) {
        for (ShopEntry e : entries) {
            if (e.getId().equals(id)) return e;
        }
        return null;
    }

    public int getMaxPage() {
        return Math.max(1, (int) Math.ceil(entries.size() / 45.0));
    }

    // ---------- 서버 시작 시 DB에서 전체 목록 불러오기 ----------

    /** 활성 판매글을 DB에서 JPQL로 한 번에 읽어와 캐시를 채웁니다. (비동기) */
    public CompletableFuture<Void> bootstrap() {
        return shopDatabase.findAllEntries().thenAccept(list -> {
            entries.clear();
            for (ShopEntryEntity e : list) {
                entries.add(toDomain(e));
            }
            ready = true;
            plugin.getLogger().info("유저상점 판매글 " + entries.size() + "건을 불러왔습니다.");
        }).exceptionally(t -> {
            plugin.getLogger().warning("유저상점 초기 로딩 실패: " + t.getMessage());
            ready = true; // DB가 아직 없거나 실패해도 서버가 무한정 막히지 않도록
            return null;
        });
    }

    // ---------- 등록 (로컬에서 발생) ----------

    /** 새 판매글을 캐시에 즉시 반영하고, DB에 비동기로 저장합니다. */
    public CompletableFuture<Void> registerEntry(ShopEntry entry) {
        entries.add(entry);
        ShopEntryEntity entity = toEntity(entry);
        return shopDatabase.saveEntry(entity).exceptionally(t -> {
            plugin.getLogger().warning("판매글 DB 저장 실패: " + t.getMessage());
            return null;
        });
    }

    /** 판매글을 캐시에서 제거하고, DB에서도 비동기로 삭제합니다. (구매/취소 시) */
    public CompletableFuture<Void> removeEntry(ShopEntry entry) {
        entries.remove(entry);
        return shopDatabase.deleteEntry(entry.getId()).exceptionally(t -> {
            plugin.getLogger().warning("판매글 DB 삭제 실패: " + t.getMessage());
            return null;
        });
    }

    // ---------- 다른 서버로부터 온 변경사항 반영 (캐시만, DB는 원본 서버가 이미 처리함) ----------

    /** 다른 서버에서 등록된 판매글을 이 서버 캐시에도 반영합니다. */
    public CompletableFuture<Void> applyRemoteAdd(UUID listingId) {
        if (getById(listingId) != null) {
            return CompletableFuture.completedFuture(null); // 이미 있음
        }
        return shopDatabase.findEntry(listingId).thenAccept(e -> {
            if (e != null && getById(listingId) == null) {
                entries.add(toDomain(e));
            }
        });
    }

    /** 다른 서버에서 제거/만료된 판매글을 이 서버 캐시에서도 제거합니다. */
    public void applyRemoteRemove(UUID listingId) {
        entries.removeIf(e -> e.getId().equals(listingId));
    }

    /** 관리자가 초기화했을 때, 이 서버의 캐시를 비웁니다. */
    public void clearCache() {
        entries.clear();
    }

    // ---------- 관리자 초기화 (로컬에서 발생) ----------

    /** 모든 판매글을 캐시 + DB에서 제거합니다 (JPQL 일괄 삭제 한 번). */
    public CompletableFuture<Void> resetAll() {
        entries.clear();
        return shopDatabase.deleteAllEntries();
    }

    // ---------- 만료 처리 (1분마다 호출, 로컬에서 발생) ----------

    /** 만료된 판매글을 찾아 캐시에서 제거하고, DB에서도 지운 뒤 판매자 보관함(DB)으로 옮깁니다. */
    public List<ShopEntry> processExpired() {
        List<ShopEntry> expired = new ArrayList<>();
        for (ShopEntry entry : entries) {
            if (entry.isExpired()) {
                expired.add(entry);
            }
        }
        for (ShopEntry entry : expired) {
            entries.remove(entry);
            String base64 = ItemSerialization.toBase64(entry.getItem());
            shopDatabase.addReturnedItem(entry.getSellerUuid(), base64).exceptionally(t -> {
                plugin.getLogger().warning("만료 아이템 보관함 저장 실패: " + t.getMessage());
                return null;
            });
            shopDatabase.deleteEntry(entry.getId()).exceptionally(t -> {
                plugin.getLogger().warning("만료 판매글 DB 삭제 실패: " + t.getMessage());
                return null;
            });
        }
        return expired;
    }

    // ---------- 반환 보관함 (캐시 없이 DB 직접 조회, 판매자 uuid로 키가 되어 서버 무관하게 동일) ----------

    public CompletableFuture<List<ItemStack>> getReturnedItems(UUID ownerUuid) {
        return shopDatabase.findReturnedBase64(ownerUuid).thenApply(list -> {
            List<ItemStack> items = new ArrayList<>();
            for (String b64 : list) {
                try {
                    items.add(ItemSerialization.fromBase64(b64));
                } catch (Exception e) {
                    plugin.getLogger().warning("보관함 아이템 역직렬화 실패: " + e.getMessage());
                }
            }
            return items;
        });
    }

    /** oneBasedIndex 위치의 아이템을 보관함에서 꺼내(제거) 반환합니다. 없으면 null.
     *  조회+삭제가 DB 트랜잭션 하나로 처리되어(ShopDatabase.takeReturnedItemAt) 경쟁 상태가 없습니다. */
    public CompletableFuture<ItemStack> takeReturnedItem(UUID ownerUuid, int oneBasedIndex) {
        return shopDatabase.takeReturnedItemAt(ownerUuid, oneBasedIndex).thenApply(base64 -> {
            if (base64 == null) return null;
            try {
                return ItemSerialization.fromBase64(base64);
            } catch (Exception e) {
                plugin.getLogger().warning("보관함 아이템 역직렬화 실패: " + e.getMessage());
                return null;
            }
        });
    }

    // ---------- 엔티티 <-> 도메인 변환 ----------

    private ShopEntry toDomain(ShopEntryEntity e) {
        ItemStack item = ItemSerialization.fromBase64(e.getItemBase64());
        return new ShopEntry(e.getId(), item, e.getPrice(), e.getSellerUuid(), e.getSellerName(), e.getExpireAt());
    }

    private ShopEntryEntity toEntity(ShopEntry e) {
        return new ShopEntryEntity(e.getId(), ItemSerialization.toBase64(e.getItem()), e.getPrice(),
                e.getSellerUuid(), e.getSellerName(), e.getExpireAt());
    }
}