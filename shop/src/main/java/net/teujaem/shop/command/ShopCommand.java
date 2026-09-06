package net.teujaem.shop.command;

import net.teujaem.shop.Shop;
import net.teujaem.shop.listener.ShopGuiListener;
import net.teujaem.shop.manager.ShopManager;
import net.teujaem.shop.model.PriceType;
import net.teujaem.shop.model.ShopType;
import net.teujaem.shop.util.MessageUtil;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * "/상점" 명령어 전체를 처리합니다. 원본 Skript 의 명령어 트리거를 그대로 옮기되,
 * 각 하위 명령어를 별도의 private 메서드로 나눠 가독성을 높였습니다.
 */
public class ShopCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ADMIN_SUBCOMMANDS = Arrays.asList(
            "만들기", "리스트", "아이템편집", "가격편집", "거래편집", "재고편집", "재고유저거래",
            "재고시간", "시세시간", "변경률최소", "변경률최대", "삭제", "열기유저권한",
            "시세변경", "재고변경", "타입변경", "잠금"
    );
    private static final List<String> NAME_REQUIRED = Arrays.asList(
            "열기", "만들기", "아이템편집", "가격편집", "거래편집", "재고편집", "재고유저거래",
            "재고시간", "시세시간", "변경률최소", "변경률최대", "삭제", "타입변경", "잠금"
    );

    private final Shop plugin;
    private final ShopManager shopManager;
    private final ShopGuiListener guiListener;

    public ShopCommand(Shop plugin) {
        this.plugin = plugin;
        this.shopManager = plugin.getShopManager();
        this.guiListener = plugin.getGuiListener();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0];
        if (!isKnownCommand(sub)) {
            MessageUtil.error(sender, "올바른 명령어를 입력해주세요.");
            return true;
        }

        String id = args.length > 1 ? args[1] : null;
        if (NAME_REQUIRED.contains(sub)) {
            if (id == null) {
                MessageUtil.error(sender, "상점 이름을 입력해주세요.");
                return true;
            }
            boolean isCreate = sub.equals("만들기");
            if (!isCreate && !shopManager.exists(id)) {
                MessageUtil.error(sender, "해당 상점은 존재하지 않습니다.");
                return true;
            }
            if (isCreate && shopManager.exists(id)) {
                MessageUtil.error(sender, "해당 상점은 이미 존재합니다.");
                return true;
            }
        }

        if (sub.equals("열기")) {
            handleOpen(sender, id);
            return true;
        }

        if (!sender.isOp() && !sender.hasPermission("shop.admin")) {
            MessageUtil.error(sender, "해당 명령어를 실행할 권한이 없습니다.");
            return true;
        }

        switch (sub) {
            case "만들기": handleCreate(sender, id, args); break;
            case "리스트": handleList(sender); break;
            case "아이템편집": requirePlayer(sender, p -> guiListener.openItemEdit(p, shopManager.get(id).get(), 1)); break;
            case "가격편집": handlePriceEdit(sender, id, args); break;
            case "거래편집": requirePlayer(sender, p -> guiListener.openTradeSetting(p, shopManager.get(id).get())); break;
            case "재고편집": requirePlayer(sender, p -> guiListener.openStockSetting(p, shopManager.get(id).get())); break;
            case "재고유저거래": handleStockUserSell(sender, id, args); break;
            case "재고시간": handleStockTime(sender, id, args); break;
            case "시세시간": handleChangeTime(sender, id, args); break;
            case "변경률최소": handleChangeRate(sender, id, args, true); break;
            case "변경률최대": handleChangeRate(sender, id, args, false); break;
            case "삭제": handleDelete(sender, id); break;
            case "열기유저권한": handleOpenPermission(sender, id); break;
            case "시세변경": handleChangePrice(sender, id); break;
            case "재고변경": handleStockRefresh(sender, id); break;
            case "타입변경": handleTypeChange(sender, id, args); break;
            case "잠금": handleLock(sender, id, args); break;
            default: break;
        }
        return true;
    }

    // ---------------- 유저 명령어 ----------------

    private void handleOpen(CommandSender sender, String id) {
        if (!(sender instanceof Player)) {
            MessageUtil.error(sender, "플레이어만 사용할 수 있는 명령어입니다.");
            return;
        }
        Player player = (Player) sender;
        if (!shopManager.isOpenEnabled() && !player.isOp() && !player.hasPermission("shop.admin")) {
            MessageUtil.error(player, "해당 명령어를 실행할 권한이 없습니다.");
            return;
        }
        if (id == null) {
            MessageUtil.error(player, "상점 이름을 입력해주세요.");
            return;
        }
        net.teujaem.shop.model.Shop shop = shopManager.get(id).orElse(null);
        if (shop == null) {
            MessageUtil.error(player, "해당 상점은 존재하지 않습니다.");
            return;
        }
        if (shop.isLocked()) {
            MessageUtil.error(player, "해당 상점은 현재 열 수 없습니다.");
            return;
        }
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        guiListener.openMain(player, shop, 1);
    }

    // ---------------- 관리자 명령어 ----------------

    private void handleCreate(CommandSender sender, String id, String[] args) {
        ShopType type = ShopType.NORMAL;
        if (args.length > 2) {
            ShopType parsed = ShopType.fromDisplayName(args[2]);
            if (parsed == null) {
                MessageUtil.error(sender, "올바른 타입을 입력해주세요.");
                return;
            }
            type = parsed;
        }
        shopManager.create(id, type);
        shopManager.save();
        MessageUtil.shop(sender, id + " 상점이 생성되었습니다.");
    }

    private void handleList(CommandSender sender) {
        MessageUtil.shop(sender, "상점 리스트");
        for (net.teujaem.shop.model.Shop shop : shopManager.all()) {
            MessageUtil.shop(sender, " - " + shop.getId());
        }
    }

    private void handlePriceEdit(CommandSender sender, String id, String[] args) {
        if (args.length <= 2) {
            MessageUtil.error(sender, "가격 타입(구매/판매)을 입력해주세요.");
            return;
        }
        PriceType type = PriceType.fromDisplayName(args[2]);
        if (type == null) {
            MessageUtil.error(sender, "올바른 타입을 입력해주세요.");
            return;
        }
        requirePlayer(sender, p -> guiListener.openPriceSetting(p, shopManager.get(id).get(), type));
    }

    private void handleStockUserSell(CommandSender sender, String id, String[] args) {
        Boolean on = parseToggle(args, 2);
        if (on == null) {
            MessageUtil.error(sender, "올바른 타입을 입력해주세요.");
            return;
        }
        shopManager.get(id).get().setStockAddOnUserSell(on);
        shopManager.save();
        MessageUtil.shop(sender, id + "의 유저 판매시 재고 증가 설정이 " + (on ? "켜짐" : "꺼짐") + "으로 변경되었습니다.");
    }

    private void handleStockTime(CommandSender sender, String id, String[] args) {
        Integer minutes = parseTime(sender, args);
        if (minutes == null) return;
        net.teujaem.shop.model.Shop shop = shopManager.get(id).get();
        shop.setStockTimeMinutes(minutes);
        shopManager.save();
        if (minutes == 0) {
            MessageUtil.shop(sender, id + "의 재고 자동 초기화가 비활성화되었습니다.");
        } else {
            MessageUtil.shop(sender, id + "의 재고가 " + minutes + "분마다 초기화됩니다.");
        }
    }

    private void handleChangeTime(CommandSender sender, String id, String[] args) {
        Integer minutes = parseTime(sender, args);
        if (minutes == null) return;
        net.teujaem.shop.model.Shop shop = shopManager.get(id).get();
        shop.setChangeTimeMinutes(minutes);
        shopManager.save();
        if (minutes == 0) {
            MessageUtil.shop(sender, id + "의 시세 자동 변경이 비활성화되었습니다.");
        } else {
            MessageUtil.shop(sender, id + "의 시세가 " + minutes + "분마다 변경됩니다.");
        }
    }

    private Integer parseTime(CommandSender sender, String[] args) {
        if (args.length < 5) {
            MessageUtil.error(sender, "일, 시, 분을 모두 입력해주세요.");
            return null;
        }
        try {
            int day = Integer.parseInt(args[2]);
            int hour = Integer.parseInt(args[3]);
            int minute = Integer.parseInt(args[4]);
            return day * 24 * 60 + hour * 60 + minute;
        } catch (NumberFormatException e) {
            MessageUtil.error(sender, "숫자를 입력해주세요.");
            return null;
        }
    }

    private void handleChangeRate(CommandSender sender, String id, String[] args, boolean min) {
        if (args.length <= 2) {
            MessageUtil.error(sender, "숫자를 입력해주세요");
            return;
        }
        Integer amount = parseInt(args[2]);
        if (amount == null) {
            MessageUtil.error(sender, "숫자를 입력해주세요");
            return;
        }
        net.teujaem.shop.model.Shop shop = shopManager.get(id).get();
        if (min) shop.setChangeRateMin(amount);
        else shop.setChangeRateMax(amount);
        shopManager.save();
        MessageUtil.shop(sender, id + "의 변경률 " + (min ? "최소값" : "최대값") + "이 " + amount + "%로 설정했습니다.");
        if (sender instanceof Player) {
            ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }
    }

    private void handleDelete(CommandSender sender, String id) {
        shopManager.delete(id);
        shopManager.save();
        MessageUtil.shop(sender, id + " 상점이 삭제되었습니다.");
    }

    private void handleOpenPermission(CommandSender sender, String toggleText) {
        Boolean on = toggleText == null ? null
                : toggleText.equals("켜기") ? Boolean.TRUE
                : toggleText.equals("끄기") ? Boolean.FALSE : null;
        if (on == null) {
            MessageUtil.error(sender, "올바른 타입을 입력해주세요.");
            return;
        }
        shopManager.setOpenEnabled(on);
        shopManager.save();
        MessageUtil.shop(sender, "유저의 상점 열기 권한이 " + (on ? "켜짐" : "꺼짐") + "으로 변경되었습니다.");
    }

    private void handleChangePrice(CommandSender sender, String id) {
        if (id != null) {
            net.teujaem.shop.model.Shop shop = shopManager.get(id).orElse(null);
            if (shop == null) {
                MessageUtil.error(sender, "해당 상점은 존재하지 않습니다.");
                return;
            }
            if (shop.getType() != ShopType.CHANGE) {
                MessageUtil.error(sender, "해당 상점의 타입이 시세가 아닙니다.");
                return;
            }
            plugin.getScheduleManager().changePrice(shop);
        } else {
            for (net.teujaem.shop.model.Shop shop : shopManager.all()) {
                if (shop.getType() == ShopType.CHANGE) {
                    plugin.getScheduleManager().changePrice(shop);
                }
            }
        }
        shopManager.save();
        MessageUtil.shop(sender, "시세를 변경했습니다.");
    }

    private void handleStockRefresh(CommandSender sender, String id) {
        if (id != null) {
            net.teujaem.shop.model.Shop shop = shopManager.get(id).orElse(null);
            if (shop == null) {
                MessageUtil.error(sender, "해당 상점은 존재하지 않습니다.");
                return;
            }
            plugin.getScheduleManager().resetStock(shop);
        } else {
            for (net.teujaem.shop.model.Shop shop : shopManager.all()) {
                plugin.getScheduleManager().resetStock(shop);
            }
        }
        shopManager.save();
        MessageUtil.shop(sender, "재고를 초기화했습니다.");
    }

    private void handleTypeChange(CommandSender sender, String id, String[] args) {
        if (args.length <= 2) {
            MessageUtil.error(sender, "타입을 입력해주세요.");
            return;
        }
        ShopType type = ShopType.fromDisplayName(args[2]);
        if (type == null) {
            MessageUtil.error(sender, "올바른 타입을 입력해주세요.");
            return;
        }
        shopManager.get(id).get().setType(type);
        shopManager.save();
        MessageUtil.shop(sender, id + "상점의 타입이 " + args[2] + "으로 변경되었습니다.");
    }

    private void handleLock(CommandSender sender, String id, String[] args) {
        Boolean on = parseToggle(args, 2);
        if (on == null) {
            MessageUtil.error(sender, "올바른 타입을 입력해주세요.");
            return;
        }
        shopManager.get(id).get().setLocked(on);
        shopManager.save();
        MessageUtil.shop(sender, id + " 상점이 " + (on ? "잠겼습니다." : "잠금 해제되었습니다."));
    }

    // ---------------- 유틸 ----------------

    private Boolean parseToggle(String[] args, int index) {
        if (args.length <= index) return null;
        if (args[index].equals("켜기")) return true;
        if (args[index].equals("끄기")) return false;
        return null;
    }

    private Integer parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void requirePlayer(CommandSender sender, java.util.function.Consumer<Player> consumer) {
        if (!(sender instanceof Player)) {
            MessageUtil.error(sender, "플레이어만 사용할 수 있는 명령어입니다.");
            return;
        }
        consumer.accept((Player) sender);
    }

    private boolean isKnownCommand(String sub) {
        return sub.equals("열기") || ADMIN_SUBCOMMANDS.contains(sub);
    }

    private void sendHelp(CommandSender sender) {
        MessageUtil.help(sender, "- 유저 명령어 -");
        if (shopManager.isOpenEnabled() || sender.isOp() || sender.hasPermission("shop.admin")) {
            MessageUtil.help(sender, "/상점 열기 <이름>");
        }
        if (sender.isOp() || sender.hasPermission("shop.admin")) {
            MessageUtil.help(sender, "- 관리자 명령어 -");
            MessageUtil.help(sender, "/상점 만들기 <이름> [일반/시세/거래] (기본값 : 일반)");
            MessageUtil.help(sender, "/상점 리스트");
            MessageUtil.help(sender, "/상점 아이템편집 <이름>");
            MessageUtil.help(sender, "/상점 가격편집 <이름> <구매/판매>");
            MessageUtil.help(sender, "/상점 거래편집 <이름>");
            MessageUtil.help(sender, "/상점 재고편집 <이름>");
            MessageUtil.help(sender, "/상점 재고유저거래 <이름> <켜기/끄기>");
            MessageUtil.help(sender, "/상점 재고시간 <이름> <일> <시> <분>");
            MessageUtil.help(sender, "/상점 시세시간 <이름> <일> <시> <분>");
            MessageUtil.help(sender, "/상점 변경률최소 <이름> <자연수>");
            MessageUtil.help(sender, "/상점 변경률최대 <이름> <자연수>");
            MessageUtil.help(sender, "/상점 삭제 <이름>");
            MessageUtil.help(sender, "/상점 열기유저권한 <켜기/끄기>");
            MessageUtil.help(sender, "/상점 시세변경 [이름]");
            MessageUtil.help(sender, "/상점 재고변경 [이름]");
            MessageUtil.help(sender, "/상점 타입변경 <이름> <일반/시세/거래>");
            MessageUtil.help(sender, "/상점 잠금 <이름> <켜기/끄기>");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("열기");
            if (sender.isOp() || sender.hasPermission("shop.admin")) {
                options.addAll(ADMIN_SUBCOMMANDS);
            }
        } else if (args.length == 2 && NAME_REQUIRED.contains(args[0])) {
            options.addAll(shopManager.all().stream().map(net.teujaem.shop.model.Shop::getId).collect(Collectors.toList()));
        } else if (args.length == 2 && args[0].equals("열기유저권한")) {
            options.addAll(Arrays.asList("켜기", "끄기"));
        } else if (args.length == 3) {
            switch (args[0]) {
                case "만들기":
                case "타입변경":
                    options.addAll(Arrays.asList("일반", "시세", "거래"));
                    break;
                case "가격편집":
                    options.addAll(Arrays.asList("구매", "판매"));
                    break;
                case "재고유저거래":
                case "잠금":
                    options.addAll(Arrays.asList("켜기", "끄기"));
                    break;
                default:
                    break;
            }
        }
        String current = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(current)).collect(Collectors.toList());
    }
}
