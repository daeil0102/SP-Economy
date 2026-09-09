package net.teujaem.usershop.config;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class ShopConfig {

    private final JavaPlugin plugin;

    public ShopConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public int getFeePercent() {
        return plugin.getConfig().getInt("fee-percent", 5);
    }

    public void setFeePercent(int percent) {
        plugin.getConfig().set("fee-percent", percent);
        plugin.saveConfig();
    }

    public int getExpireDays() {
        return plugin.getConfig().getInt("expire.days", 2);
    }

    public int getExpireHours() {
        return plugin.getConfig().getInt("expire.hours", 0);
    }

    public int getExpireMinutes() {
        return plugin.getConfig().getInt("expire.minutes", 0);
    }

    public void setExpire(int days, int hours, int minutes) {
        plugin.getConfig().set("expire.days", days);
        plugin.getConfig().set("expire.hours", hours);
        plugin.getConfig().set("expire.minutes", minutes);
        plugin.saveConfig();
    }

    /** 기본 만료 기간(ms) */
    public long getDefaultExpireDurationMillis() {
        return ((long) getExpireDays() * 24 * 60 + getExpireHours() * 60L + getExpireMinutes()) * 60_000L;
    }

    public String getServerName() {
        return plugin.getConfig().getString("server-name", "server-1");
    }

    public String prefixShop() {
        return color(plugin.getConfig().getString("messages.prefix-shop", "&6&l[&a&lUserShop&6&l]&f"));
    }

    public String prefixHelp() {
        return color(plugin.getConfig().getString("messages.prefix-help", "&c&l[&b&lUserShop Help&c&l]&f"));
    }

    public String prefixError() {
        return color(plugin.getConfig().getString("messages.prefix-error", "&4&l[&c&l오류&4&l]&f"));
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}
