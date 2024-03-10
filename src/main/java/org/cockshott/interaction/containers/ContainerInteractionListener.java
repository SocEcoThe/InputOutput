package org.cockshott.interaction.containers;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.cockshott.cache.CacheManager;

public class ContainerInteractionListener implements Listener {
    private final CacheManager cacheManager;

    public ContainerInteractionListener(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        // 处理容器打开事件，标记开始存储或存储转移
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // 处理容器关闭事件，更新缓存数据
    }
}

