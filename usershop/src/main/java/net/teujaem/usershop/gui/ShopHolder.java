package net.teujaem.usershop.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * "유저상점" GUI라는 것과, 현재 몇 페이지를 보고 있는지를 인벤토리 이름 파싱 없이
 * 안전하게 들고 있기 위한 홀더.
 */
public class ShopHolder implements InventoryHolder {

    private Inventory inventory;
    private int page;

    public ShopHolder(int page) {
        this.page = page;
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
