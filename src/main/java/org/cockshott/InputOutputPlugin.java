package org.cockshott;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.Iterator;

import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.cockshott.cache.CacheManager;
import org.cockshott.database.DatabaseManager;
import org.cockshott.interaction.containers.ContainerInteractionListener;
import org.cockshott.interaction.containers.InventoryChangeListener;
import org.cockshott.interaction.drop.DropItem;
import org.cockshott.interaction.drop.ItemDropListener;

public class InputOutputPlugin extends JavaPlugin implements Listener {
    private CacheManager cacheManager; // 用于管理数据缓存
    private DatabaseManager databaseManager; // 用于管理数据库交互
    // 维护一个丢弃列表
    private final List<DropItem> droppedItems = Collections.synchronizedList(new ArrayList<>());
    private boolean hasInitialized = false;

    @Override
    public void onEnable() {
//        getServer().getPluginManager().registerEvents(this, this);
        initializePlugin();
//        new BukkitRunnable() {
//            @Override
//            public void run() {
//                // 打印所有玩家记录
//                cacheManager.getAllPlayerTransactions().forEach((uuid, transactions) -> {
//                    System.out.println("玩家ID" + uuid + "；操作记录：" + transactions.toString());
//                });
//            }
//        }.runTaskTimerAsynchronously(this, 0L, 20L * 30); // 20 ticks * 30 seconds

        // 启动定时任务，同步缓存到数据库
        // new BukkitRunnable() {
        // @Override
        // public void run() {
        // databaseManager.syncFromCache(cacheManager);
        // }
        // }.runTaskTimerAsynchronously(this, 20L * 60, 20L * 60); // 每分钟同步一次
    }

    private void initializePlugin() {
        // 初始化缓存管理器和数据库管理器
        this.cacheManager = new CacheManager();
        this.databaseManager = new DatabaseManager();

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new InventoryChangeListener(this), this);
//        getServer().getPluginManager().registerEvents(new PlayerInventoryListener(cacheManager,this), this);
        getServer().getPluginManager().registerEvents(new ContainerInteractionListener(cacheManager), this);
        getServer().getPluginManager().registerEvents(new ItemDropListener(this), this);
        getServer().getScheduler().runTaskTimerAsynchronously(this, this::checkDroppedItems, 0L, 600L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 检查是否已经重载过
        if (!hasInitialized) {
            hasInitialized = true;
            initializePlugin();
        }
    }

    @Override
    public void onDisable() {
        // 插件禁用时确保数据完全同步
//        databaseManager.syncFromCache(cacheManager);
    }

    public void addItemToCheck(DropItem item) {
        droppedItems.add(item);
    }

    public void checkDroppedItems() {
        Iterator<DropItem> iterator = droppedItems.iterator();
        synchronized (droppedItems) {
            while (iterator.hasNext()) {
                DropItem dropItem = iterator.next();
                Item item = dropItem.getItem();
                UUID uuid = dropItem.getPlayerUUID();
                if (item.isDead() || !item.isValid()) {
                    // 处理已经消失的物品（被拾取或自然消失）
                    // 例如更新缓存或记录数据
                    iterator.remove(); // 从列表中移除
//                    cacheManager.recordOutput(uuid, item.getItemStack());
                } else {
                    // 如果物品仍然存在，处理逻辑，例如标记为“投入”
                    iterator.remove(); // 检查后从列表中移除
                }
            }
        }
    }
}
