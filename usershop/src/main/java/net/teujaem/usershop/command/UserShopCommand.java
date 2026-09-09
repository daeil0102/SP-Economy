package net.teujaem.usershop.command;

import net.teujaem.money.api.Money;
import net.teujaem.usershop.UserShop;
import net.teujaem.usershop.config.ShopConfig;
import net.teujaem.usershop.gui.GuiBuilder;
import net.teujaem.usershop.manager.ShopManager;
import net.teujaem.usershop.model.ShopEntry;
import net.teujaem.usershop.model.ShopUpdateInfo;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserShopCommand implements CommandExecutor, TabCompleter {

    private final UserShop plugin;

    public UserShopCommand(UserShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ShopConfig config = plugin.getShopConfig();

        if (args.length == 0) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "현재 유저상점 만료일은 &c" + config.getExpireDays() + "일 " + config.getExpireHours()
                            + "시 " + config.getExpireMinutes() + "분 &f입니다."));
            sender.sendMessage("");
            sender.sendMessage(config.prefixHelp() + " /유저상점 열기");
            sender.sendMessage(config.prefixHelp() + " /유저상점 보관함");
            sender.sendMessage(config.prefixHelp() + " /유저상점 등록 [금액] [갯수]");
            if (sender.hasPermission("usershop.admin")) {
                sender.sendMessage(ChatColor.RED + "[관리자 전용]");
                sender.sendMessage(config.prefixHelp() + " /유저상점 수수료 [숫자]");
                sender.sendMessage(config.prefixHelp() + " /유저상점 초기화");
                sender.sendMessage(config.prefixHelp() + " /유저상점 만료일설정 [일] [시] [분]");
            }
            return true;
        }

        String sub = args[0];
        switch (sub) {
            case "열기" -> openShop(sender);
            case "보관함" -> openStorage(sender);
            case "등록" -> register(sender, args);
            case "수수료" -> setFee(sender, args);
            case "초기화" -> resetShop(sender);
            case "만료일설정" -> setExpire(sender, args);
            default -> sender.sendMessage(config.prefixError() + " 알 수 없는 명령어입니다.");
        }
        return true;
    }

    private void openShop(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return;
        }
        if (!plugin.getShopManager().isReady()) {
            sender.sendMessage(plugin.getShopConfig().prefixError() + " 상점을 불러오는 중입니다. 잠시 후 다시 시도해주세요.");
            return;
        }
        player.openInventory(GuiBuilder.buildShop(player, plugin.getShopManager(), 1));
    }

    private void openStorage(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return;
        }
        UUID uuid = player.getUniqueId();
        plugin.getShopManager().getReturnedItems(uuid).thenAccept(items ->
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.openInventory(GuiBuilder.buildStorage(player, uuid, items, 1)))
        ).exceptionally(t -> {
            plugin.getLogger().warning("보관함 조회 실패: " + t.getMessage());
            return null;
        });
    }

    private void register(CommandSender sender, String[] args) {
        ShopConfig config = plugin.getShopConfig();
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            sender.sendMessage(config.prefixError() + " 손의 아이템을 들어주세요.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(config.prefixError() + " 가격을 설정해 주세요");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(config.prefixError() + " 갯수를 설정해 주세요");
            return;
        }

        int price;
        int amount;
        try {
            price = Integer.parseInt(args[1]);
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(config.prefixError() + " 숫자를 정확히 입력해주세요.");
            return;
        }

        if (price <= 0 || amount <= 0) {
            sender.sendMessage(config.prefixError() + " 가격과 갯수는 1 이상이어야 합니다.");
            return;
        }

        int handAmount = hand.getAmount();
        if (amount > handAmount) {
            sender.sendMessage(config.prefixError() + " 당신은 손에 " + amount + "개보다 적게 가지고 있으므로 등록하실 수 없습니다.");
            return;
        }

        int fee = price * config.getFeePercent() / 100;
        if (Money.get(player) < fee) {
            sender.sendMessage(config.prefixError() + " 수수료를 낼 돈이 없어서 유저상점에 물건을 올릴 수 없습니다.");
            return;
        }

        ItemStack toSell = hand.clone();
        toSell.setAmount(amount);
        hand.setAmount(handAmount - amount);
        player.getInventory().setItemInMainHand(hand.getAmount() <= 0 ? null : hand);

        Money.remove(player, fee);

        long expireAt = System.currentTimeMillis() + config.getDefaultExpireDurationMillis();
        ShopEntry entry = new ShopEntry(UUID.randomUUID(), toSell, price, player.getUniqueId(), player.getName(), expireAt);
        plugin.getShopManager().registerEntry(entry);
        plugin.broadcastShopUpdate(ShopUpdateInfo.Type.ADD, entry.getId(), entry.getSellerUuid());

        sender.sendMessage(config.prefixShop() + " 유저상점에 등록이 완료되었습니다.");
    }

    private void setFee(CommandSender sender, String[] args) {
        ShopConfig config = plugin.getShopConfig();
        if (!sender.hasPermission("usershop.admin")) {
            sender.sendMessage(config.prefixError() + " 권한이 없습니다.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(config.prefixError() + " 숫자를 입력해주세요.");
            return;
        }
        try {
            int percent = Integer.parseInt(args[1]);
            config.setFeePercent(percent);
            sender.sendMessage(config.prefixShop() + " 수수료가 " + percent + "%로 설정되었습니다.");
        } catch (NumberFormatException ex) {
            sender.sendMessage(config.prefixError() + " 숫자를 입력해주세요.");
        }
    }

    private void resetShop(CommandSender sender) {
        ShopConfig config = plugin.getShopConfig();
        if (!sender.hasPermission("usershop.admin")) {
            sender.sendMessage(config.prefixError() + " 권한이 없습니다.");
            return;
        }
        ShopManager shopManager = plugin.getShopManager();
        shopManager.resetAll().thenRun(() ->
                plugin.broadcastShopUpdate(ShopUpdateInfo.Type.RESET, null, null)
        ).exceptionally(t -> {
            plugin.getLogger().warning("상점 초기화 실패: " + t.getMessage());
            return null;
        });
        sender.sendMessage(config.prefixShop() + " 모든 거래물품이 초기화 되었습니다.");
    }

    private void setExpire(CommandSender sender, String[] args) {
        ShopConfig config = plugin.getShopConfig();
        if (!sender.hasPermission("usershop.admin")) {
            sender.sendMessage(config.prefixError() + " 권한이 없습니다.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(config.prefixError() + " 일을 입력해주세요");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(config.prefixError() + " 시를 입력해주세요");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(config.prefixError() + " 분을 입력해주세요");
            return;
        }
        try {
            int days = Integer.parseInt(args[1]);
            int hours = Integer.parseInt(args[2]);
            int minutes = Integer.parseInt(args[3]);
            if (hours >= 24) {
                sender.sendMessage(config.prefixError() + " 시간은 24시 미만으로 설정해주세요");
                return;
            }
            if (minutes >= 60) {
                sender.sendMessage(config.prefixError() + " 분은 60분 미만으로 설정해주세요");
                return;
            }
            config.setExpire(days, hours, minutes);
            sender.sendMessage(config.prefixShop() + " 만료일이 " + days + "일 " + hours + "시 " + minutes + "분으로 설정되었습니다.");
        } catch (NumberFormatException ex) {
            sender.sendMessage(config.prefixError() + " 숫자를 정확히 입력해주세요.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("열기");
            options.add("보관함");
            options.add("등록");
            if (sender.hasPermission("usershop.admin")) {
                options.add("수수료");
                options.add("초기화");
                options.add("만료일설정");
            }
            List<String> result = new ArrayList<>();
            for (String option : options) {
                if (option.startsWith(args[0])) result.add(option);
            }
            return result;
        }
        return List.of();
    }
}
