package org.cockshott.interaction;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.cockshott.cache.CacheManager;
import org.cockshott.tools.InventorySnapshot;

import java.util.HashMap;
import java.util.Map;

public class InventoryChangeListener implements Listener {
    private final Map<Player, Map<String, Integer>> playerSnapshots = new HashMap<>();
    private final JavaPlugin plugin;
    private final InventorySnapshot inventorySnapshot;

    public InventoryChangeListener(JavaPlugin plugin, CacheManager cacheManager) {
        this.plugin = plugin;
        this.inventorySnapshot = new InventorySnapshot(cacheManager);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        new BukkitRunnable() {
            public void run() {
                if (!event.getPlayer().isOnline()) {
                    this.cancel();
                    return;
                }
                // 创建或更新玩家的背包快照
                contrast(event.getPlayer());
            }
        }.runTaskTimer(plugin, 0L, 100L); // 0L 是延迟时间，600L 是30秒的tick时间
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 玩家下线时检查一次
        contrast(event.getPlayer());
    }

    public void contrast(Player player){
        if (playerSnapshots.containsKey(player)) {
            // 比较之前和当前的背包状态
            Map<String, Integer> beforeSnapshot = playerSnapshots.get(player);
            Map<String, Integer> afterSnapshot = inventorySnapshot.takeSnapshot(player);
            inventorySnapshot.compareSnapshots(beforeSnapshot, afterSnapshot, player);
            // 更新快照以便下一次比较
            playerSnapshots.put(player, afterSnapshot);
        }else {
            playerSnapshots.put(player,inventorySnapshot.takeSnapshot(player));
        }
    }
}

