package net.teujaem.usershop.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * "반환된 아이템" 보관함 GUI 홀더. 어떤 판매자(uuid)의 보관함인지와 페이지를 들고 있습니다.
 */
public class StorageHolder implements InventoryHolder {

    private Inventory inventory;
    private final UUID ownerUuid;
    private int page;

    public StorageHolder(UUID ownerUuid, int page) {
        this.ownerUuid = ownerUuid;
        this.page = page;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
