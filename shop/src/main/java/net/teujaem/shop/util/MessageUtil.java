package net.teujaem.shop.util;

import net.teujaem.shop.Shop;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class MessageUtil {

    private MessageUtil() {
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private static String prefix(String path) {
        return color(Shop.getInstance().getConfig().getString("messages." + path, ""));
    }

    public static void shop(CommandSender target, String message) {
        target.sendMessage(prefix("prefix-shop") + color(message));
    }

    public static void help(CommandSender target, String message) {
        target.sendMessage(prefix("prefix-help") + color(message));
    }

    public static void error(CommandSender target, String message) {
        target.sendMessage(prefix("prefix-error") + color(message));
    }

    public static String guiTitle(String key, String suffix) {
        String base = color(Shop.getInstance().getConfig().getString("gui-titles." + key, key));
        return suffix == null ? base : base + suffix;
    }
}
