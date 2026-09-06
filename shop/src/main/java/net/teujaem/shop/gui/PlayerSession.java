package net.teujaem.shop.gui;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 플레이어별 GUI 진행 상태. 원본 Skript 의 {shop.setting.*::uuid} 전역 변수들을
 * 하나의 객체로 모아서 관리합니다. 상점 top-level GUI 를 완전히 벗어날 때 초기화됩니다.
 */
public class PlayerSession {

    private final StringBuilder keypadInput = new StringBuilder();
    private Long pendingNumber;              // 키패드 확인(confirm) 후 저장되는 값
    private final List<ItemStack> pendingTradeItems = new ArrayList<>();
    private boolean changingPage = false;    // 페이지 이동으로 인한 close 인지 구분

    public String getKeypadDisplay() {
        return keypadInput.length() == 0 ? "0" : keypadInput.toString();
    }

    public void appendDigit(char digit) {
        if (keypadInput.length() >= 9) return; // int 오버플로 방지
        keypadInput.append(digit);
    }

    public void backspace() {
        if (keypadInput.length() > 0) {
            keypadInput.deleteCharAt(keypadInput.length() - 1);
        }
    }

    public void resetKeypad() {
        keypadInput.setLength(0);
    }

    public long confirmKeypad() {
        long value = keypadInput.length() == 0 ? 0L : Long.parseLong(keypadInput.toString());
        pendingNumber = value;
        resetKeypad();
        return value;
    }

    public Long getPendingNumber() {
        return pendingNumber;
    }

    public void clearPendingNumber() {
        pendingNumber = null;
    }

    public List<ItemStack> getPendingTradeItems() {
        return pendingTradeItems;
    }

    public void setPendingTradeItems(List<ItemStack> items) {
        pendingTradeItems.clear();
        pendingTradeItems.addAll(items);
    }

    public void clearPendingTradeItems() {
        pendingTradeItems.clear();
    }

    public boolean isChangingPage() {
        return changingPage;
    }

    public void setChangingPage(boolean changingPage) {
        this.changingPage = changingPage;
    }
}
