package org.cockshott.tools;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.cockshott.cache.CacheManager;

import java.util.HashMap;
import java.util.Map;

public class InventorySnapshot {
    private static final CacheManager cacheManager = new CacheManager();
    // 记录玩家背包的状态

    public static Map<String, Integer> takeSnapshot(Player player) {
        Map<String, Integer> snapshot = new HashMap<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !ItemUtils.getType(item).contains("AIR")) {
                snapshot.merge(ItemUtils.getType(item), ItemUtils.getAmount(item), Integer::sum);
            }
        }
        return snapshot;
    }

    // 比较两个背包状态
    public static void compareSnapshots(Map<String, Integer> before, Map<String, Integer> after, Player player) {
        // 检查产出（在 after 中找 before 没有的或数量更多的物品）
        after.forEach((item, quantity) -> {
            int beforeQuantity = before.getOrDefault(item, 0);
            if (quantity > beforeQuantity) {
                cacheManager.recordOutput(player.getUniqueId(),quantity - beforeQuantity,item);
                System.out.println(player.getName() + " 产出了: " + item + " x " + (quantity - beforeQuantity));
            }
        });

        // 检查投入（在 before 中找 after 没有的或数量更少的物品）
        before.forEach((item, quantity) -> {
            int afterQuantity = after.getOrDefault(item, 0);
            if (quantity > afterQuantity) {
                cacheManager.recordInput(player.getUniqueId(),quantity - afterQuantity,item);
                System.out.println(player.getName() + " 投入了: " + item + " x " + (quantity - afterQuantity));
            }
        });
    }
}

