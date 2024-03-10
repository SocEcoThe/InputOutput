package org.cockshott;

import java.util.*;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.cockshott.cache.CacheManager;
import org.cockshott.cache.ItemOperation;
import org.cockshott.database.DatabaseManager;
import org.cockshott.interaction.InventoryChangeListener;

public class InputOutputPlugin extends JavaPlugin implements Listener {
    private CacheManager cacheManager; // 用于管理数据缓存
    private DatabaseManager databaseManager;
    private final List<ItemOperation> playerInteraction = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void onEnable() {
        initializePlugin();
    }

    private void initializePlugin() {
        // 初始化缓存管理器和数据库管理器
        this.cacheManager = new CacheManager(playerInteraction);
        this.databaseManager = new DatabaseManager();

        databaseManager.initialization();

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new InventoryChangeListener(this,cacheManager), this);

         // 启动定时任务，同步缓存到数据库
         new BukkitRunnable() {
         @Override
         public void run() {
             synchronized (playerInteraction){
                 try {
                     databaseManager.uploadItemOperations(playerInteraction);
                     playerInteraction.clear();
                 } catch (Exception e) {
                     throw new RuntimeException(e);
                 }
             }
         }
         }.runTaskTimerAsynchronously(this, 0, 20L * 10); // 每分钟同步一次
    }

    @Override
    public void onDisable() {
        // 插件关闭时确保数据完全同步
        try {
            databaseManager.uploadItemOperations(playerInteraction);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
