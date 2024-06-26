package org.cockshott.tools;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.cockshott.cache.CacheManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class InventorySnapshot {
    private final CacheManager cacheManager;

    public InventorySnapshot(CacheManager cacheManager){
        this.cacheManager = cacheManager;
    }
    // 记录玩家背包的状态

    public Map<String, Integer> takeSnapshot(Player player) {
        Map<String, Integer> snapshot = new HashMap<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !ItemUtils.getType(item).contains("AIR")) {
                snapshot.merge(ItemUtils.getType(item), ItemUtils.getAmount(item), Integer::sum);
            }
        }
        return snapshot;
    }

    // 比较两个背包状态
    public void compareSnapshots(Map<String, Integer> before, Map<String, Integer> after, Map <UUID, Map<String, Integer>> playerPlace, Player player, Boolean isStore) {
        // 检查产出（在 after 中找 before 没有的或数量更多的物品）
        after.forEach((item, quantity) -> {
            int beforeQuantity = before.getOrDefault(item, 0);
            if (quantity > beforeQuantity) {
                cacheManager.recordOutput(player,(quantity - beforeQuantity),item,isStore);
            }
        });

        // 检查投入（在 before 中找 after 没有的或数量更少的物品）
        before.forEach((item, quantity) -> {
            int afterQuantity = after.getOrDefault(item, 0);
            // 获取玩家的放置记录
            Map<String, Integer> blockCounts = playerPlace.get(player.getUniqueId());

            if (blockCounts != null) {
                // 对放置记录进行模糊匹配
                Iterator<Map.Entry<String, Integer>> it = blockCounts.entrySet().iterator();

                while (it.hasNext()) {
                    Map.Entry<String, Integer> entry = it.next();
                    String placedItem = entry.getKey().toUpperCase();
                    String upperItem = item.toUpperCase();
                    int placedCount = entry.getValue();
                    boolean a = placedItem.contains(upperItem) || upperItem.contains(placedItem);
                    if (a) {
                        afterQuantity += placedCount; // 将放置数量加回afterQuantity以反映实际消耗

                        // 一旦考虑放置量，将放置的物品记录为投入
                        if (placedCount > 0) {
                            cacheManager.recordInput(player, placedCount, "PLACED:" + placedItem.replace("MINECRAFT:",""), isStore);
                            it.remove();
                        }
                    }
                }
            }

            if (quantity > afterQuantity) {
                cacheManager.recordInput(player, (quantity - afterQuantity), item, isStore);
            }
        });
    }
}

