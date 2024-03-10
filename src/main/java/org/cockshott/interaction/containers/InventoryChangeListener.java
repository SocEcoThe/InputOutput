package org.cockshott.interaction.containers;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.cockshott.tools.InventorySnapshot;

import java.util.HashMap;
import java.util.Map;

public class InventoryChangeListener implements Listener {
    private final Map<Player, Map<String, Integer>> playerSnapshots = new HashMap<>();
    private final JavaPlugin plugin;

    public InventoryChangeListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            // 记录玩家打开容器时背包的快照
            playerSnapshots.put(player, InventorySnapshot.takeSnapshot(player));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            // 确认我们之前记录了快照
            if (playerSnapshots.containsKey(player)) {
//                playerSnapshots.remove(player);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        InventoryAction action = event.getAction();
        switch (action) {
            case PLACE_ALL:
            case PLACE_SOME:
            case PLACE_ONE:
            case SWAP_WITH_CURSOR:
            case HOTBAR_SWAP:
            case HOTBAR_MOVE_AND_READD:
                contrast(player);
                break;
            case MOVE_TO_OTHER_INVENTORY:
                // 处理Shift+点击
                if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                    contrast(player);
                }
                break;
            default:
                // 其他操作不处理
                break;
        }
    }

    public void contrast(Player player){
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (playerSnapshots.containsKey(player)) {
                // 比较之前和当前的背包状态
                Map<String, Integer> beforeSnapshot = playerSnapshots.get(player);
                Map<String, Integer> afterSnapshot = InventorySnapshot.takeSnapshot(player);
                InventorySnapshot.compareSnapshots(beforeSnapshot, afterSnapshot, player);
                // 更新快照以便下一次比较
                playerSnapshots.put(player, afterSnapshot);
            }
        }, 1L); // 延迟1 tick以等待物品移动完成
    }
}

