package org.cockshott;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.cockshott.Command.UpdateConfigCommand;
import org.cockshott.Command.UpdateConfigTabCompleter;
import org.cockshott.cache.CacheManager;
import org.cockshott.cache.ItemOperation;
import org.cockshott.database.DatabaseManager;
import org.cockshott.interaction.InventoryChangeListener;

public class InputOutputPlugin extends JavaPlugin implements Listener {
    private CacheManager cacheManager; // 用于管理数据缓存
    private DatabaseManager databaseManager;
    private final List<ItemOperation> playerInteraction = Collections.synchronizedList(new ArrayList<>());
    private BukkitTask syncTask;
    private final Map<UUID, BukkitTask> playerSnapshotTasks = new HashMap<>();
    private InventoryChangeListener inventoryChangeListener;

    @Override
    public void onEnable() {
        initializePlugin();
    }

    private void initializePlugin() {
        saveDefaultConfig();
        // 初始化缓存管理器和数据库管理器
        this.cacheManager = new CacheManager(playerInteraction);

        this.databaseManager = new DatabaseManager();
        databaseManager.initialization();

        List<String> validBlocks = getConfig().getStringList("valid_blocks");
        this.inventoryChangeListener = new InventoryChangeListener(cacheManager,validBlocks);

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(inventoryChangeListener, this);
        getServer().getPluginManager().registerEvents(this, this);

        this.getCommand("updateconfig").setExecutor(new UpdateConfigCommand(this));
        this.getCommand("updateconfig").setTabCompleter(new UpdateConfigTabCompleter());

         // 启动定时任务，同步缓存到数据库
         startSyncTask();
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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        int snapshotInterval = getConfig().getInt("snapshot_interval", 60);
        long snapshotIntervalTicks = snapshotInterval * 20L; // 将秒转换为tick

        BukkitTask task = new BukkitRunnable() {
            public void run() {
                Player player = event.getPlayer();
                if (!player.isOnline()) {
                    this.cancel(); // 如果玩家不在线，则取消任务
                    return;
                }
                if (inventoryChangeListener.saveInteraction) return;
                // 创建或更新玩家的背包快照
                inventoryChangeListener.contrast(player);
            }
        }.runTaskTimer(this, 0L, snapshotIntervalTicks);

        playerSnapshotTasks.put(event.getPlayer().getUniqueId(), task); // 跟踪任务
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 取消对应玩家的定时任务
        UUID playerId = event.getPlayer().getUniqueId();
        BukkitTask task = playerSnapshotTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        // 检查一次玩家背包
        inventoryChangeListener.contrast(event.getPlayer());
    }

    public void updateSnapshotInterval() {
        // 取消所有现有任务
        playerSnapshotTasks.values().forEach(BukkitTask::cancel);
        playerSnapshotTasks.clear();

        Bukkit.getOnlinePlayers().forEach(player -> {
            // 对于每个在线玩家，重新启动快照更新任务
            onPlayerJoin(new PlayerJoinEvent(player, null));
        });
    }


    public void reloadValidBlocks() {
        saveConfig();
        // 重新加载配置文件，以确保获取的是最新的配置
        reloadConfig();
        // 从配置重新加载validBlocks
        inventoryChangeListener.validBlocks = getConfig().getStringList("valid_blocks");
    }

    public void startSyncTask() {
        if (syncTask != null && !syncTask.isCancelled()) {
            syncTask.cancel(); // 如果任务已经在运行，先取消它
        }
        long time = getConfig().getLong("upload_interval", 30) * 20L; // 从配置读取时间，转换为tick

        syncTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (playerInteraction.isEmpty())return;

                synchronized (playerInteraction){
                    try {
                        databaseManager.uploadItemOperations(playerInteraction);
                        playerInteraction.clear();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }.runTaskTimerAsynchronously(this, 0, time); // 重新启动任务
    }

    public void reloadUploadTime(){
        saveConfig();
        reloadConfig();
        startSyncTask();
    }

    public void reloadSnapshotTime(){
        saveConfig();
        reloadConfig();
        updateSnapshotInterval();
    }
}
