package net.teujaem.usershop.util;

import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * ItemStack을 YAML(String)에 안전하게 저장하기 위한 base64 직렬화 유틸.
 */
public final class ItemSerialization {

    private ItemSerialization() {
    }

    public static String toBase64(ItemStack item) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(bos)) {
            out.writeObject(item);
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("아이템 직렬화 실패", e);
        }
    }

    public static ItemStack fromBase64(String data) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream in = new BukkitObjectInputStream(bis)) {
            Object obj = in.readObject();
            if (obj instanceof ItemStack item) {
                return item;
            }
            throw new IOException("직렬화된 데이터가 ItemStack이 아닙니다.");
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("아이템 역직렬화 실패", e);
        }
    }

    static {
        // ItemStack 직렬화 등록 (일부 환경에서 필요)
        ConfigurationSerialization.registerClass(org.bukkit.inventory.ItemStack.class);
    }
}
