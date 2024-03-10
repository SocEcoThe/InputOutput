package org.cockshott.interaction.drop;

import org.bukkit.entity.Item;
import java.util.UUID;

public class DropItem {
    private final UUID playerUUID;
    private final Item item;

    // 构造方法，初始化存储的内容
    public DropItem(UUID playerUUID, Item item) {
        this.playerUUID = playerUUID;
        this.item = item;
    }

    // 获取玩家的UUID
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    // 获取丢弃的ItemStack
    public Item getItem() {
        return item;
    }
}
