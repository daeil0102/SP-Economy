package net.teujaem.shop.gui;

import net.teujaem.shop.model.PriceType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ShopInventoryHolder implements InventoryHolder {

    private final GuiType type;
    private final String shopId;
    private int page;
    private PriceType priceType;
    private Integer slotIndex;
    private Inventory inventory;
    private boolean changingPage;

    public ShopInventoryHolder(GuiType type, String shopId, int page) {
        this.type = type;
        this.shopId = shopId;
        this.page = page;
    }

    public GuiType getType() {
        return type;
    }

    public String getShopId() {
        return shopId;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public PriceType getPriceType() {
        return priceType;
    }

    public ShopInventoryHolder priceType(PriceType priceType) {
        this.priceType = priceType;
        return this;
    }

    public Integer getSlotIndex() {
        return slotIndex;
    }

    public ShopInventoryHolder slotIndex(Integer slotIndex) {
        this.slotIndex = slotIndex;
        return this;
    }

    public boolean isChangingPage() {
        return changingPage;
    }

    public void setChangingPage(boolean changingPage) {
        this.changingPage = changingPage;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}