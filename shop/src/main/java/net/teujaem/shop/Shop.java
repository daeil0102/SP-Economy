package net.teujaem.shop;

import net.teujaem.shop.command.ShopCommand;
import net.teujaem.shop.economy.EconomyService;
import net.teujaem.shop.gui.GuiFactory;
import net.teujaem.shop.gui.SessionManager;
import net.teujaem.shop.listener.ShopGuiListener;
import net.teujaem.shop.manager.ScheduleManager;
import net.teujaem.shop.manager.ShopManager;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Shop extends JavaPlugin {

    private static Shop instance;

    private EconomyService economyService;
    private ShopManager shopManager;
    private SessionManager sessionManager;
    private GuiFactory guiFactory;
    private ScheduleManager scheduleManager;
    private ShopGuiListener guiListener;

    public static Shop getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.economyService = new EconomyService();
        this.shopManager = new ShopManager(this);
        this.sessionManager = new SessionManager();
        this.guiFactory = new GuiFactory(this);
        this.scheduleManager = new ScheduleManager(this);

        shopManager.load();

        this.guiListener = new ShopGuiListener(this);
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(guiListener, this);

        ShopCommand shopCommand = new ShopCommand(this);
        getCommand("상점").setExecutor(shopCommand);
        getCommand("상점").setTabCompleter(shopCommand);

        scheduleManager.start();

        getLogger().info("ShopPlugin 이 활성화되었습니다. (SPEconomy 연동)");
    }

    @Override
    public void onDisable() {
        if (scheduleManager != null) {
            scheduleManager.stop();
        }
        if (shopManager != null) {
            shopManager.save();
        }
        getLogger().info("ShopPlugin 이 비활성화되었습니다.");
    }

    public EconomyService getEconomyService() {
        return economyService;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public GuiFactory getGuiFactory() {
        return guiFactory;
    }

    public ScheduleManager getScheduleManager() {
        return scheduleManager;
    }

    public ShopGuiListener getGuiListener() {
        return guiListener;
    }
}
