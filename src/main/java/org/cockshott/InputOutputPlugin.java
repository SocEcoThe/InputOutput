package org.cockshott;

import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import org.cockshott.cache.PlayerStats;
import org.cockshott.database.DatabaseManager;
import org.cockshott.interaction.InventoryChangeListener;

public class InputOutputPlugin extends JavaPlugin implements Listener {
    private CacheManager cacheManager; // 用于管理数据缓存
    private DatabaseManager databaseManager;
    private BukkitTask syncTask;
    private InventoryChangeListener inventoryChangeListener;
    private final List<ItemOperation> playerInteraction = Collections.synchronizedList(new ArrayList<>());
    private final Map<UUID, BukkitTask> playerSnapshotTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> playerTimeUpdateTasks = new HashMap<>();
    private final Map<UUID, PlayerStats> joinTimes = new HashMap<>();

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
        this.inventoryChangeListener = new InventoryChangeListener(cacheManager,validBlocks,this);

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
        Player player = event.getPlayer();
        updateSnapshot(player);
        UUID playerId = player.getUniqueId();
        PlayerStats stats = new PlayerStats();
        joinTimes.put(playerId, stats); // 记录上线时间
        databaseManager.recordPlayerJoin(playerId, player.getName(),stats.getJoinDate()); // 记录玩家加入信息
        BukkitTask task = new BukkitRunnable(){
            @Override
            public void run() {
                updatePlayerOnlineTime(player,false);
            }
        }.runTaskTimer(this,300*20L,300*20L);
        playerTimeUpdateTasks.put(playerId,task);
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 取消对应玩家的定时任务
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        cancelTaskFromMap(playerId,playerSnapshotTasks);
        cancelTaskFromMap(playerId,playerTimeUpdateTasks);
        // 最后检查玩家背包后清空快照和标记
        inventoryChangeListener.contrast(event.getPlayer());
        inventoryChangeListener.removePlayerSnapshots(playerId);
        inventoryChangeListener.removeSaveInteraction(playerId);
        // 更新在线时间
        updatePlayerOnlineTime(player,true);
        joinTimes.remove(playerId);
    }

    private void cancelTaskFromMap(UUID playerId, Map<UUID,BukkitTask> map){
        BukkitTask task = map.remove(playerId);
        if (task != null) task.cancel();
    }

    public void updatePlayerOnlineTime(Player player, boolean quit) {
        UUID playerId = player.getUniqueId();
        PlayerStats stats = joinTimes.getOrDefault(playerId, new PlayerStats());
        long currentTime = System.currentTimeMillis() / 1000;
        long timeDiff = currentTime - stats.getLastUpdateTime(); // 计算自上次更新以来的时间差
        stats.setOnlineTime(timeDiff); // 累加时间差到总在线时长

        // 更新数据库记录
        databaseManager.recordPlayerQuit(playerId, stats.getOnlineTime(), stats.getJoinDate());

        // 检查是否需要重置状态（例如日期变更或退出时）
        Date dateNow = Date.valueOf(LocalDate.now());
        if (!dateNow.equals(stats.getJoinDate()) && !quit) {
            stats = new PlayerStats();
            joinTimes.put(playerId, stats);
            databaseManager.recordPlayerJoin(playerId, player.getName(), dateNow);
        } else {
            // 更新最后更新时间
            stats.setLastUpdateTime(currentTime);
            joinTimes.put(playerId, stats);
        }
    }

    public void updateSnapshot(Player player){
        int snapshotInterval = getConfig().getInt("snapshot_interval", 60);
        long snapshotIntervalTicks = snapshotInterval * 20L; // 将秒转换为tick
        UUID playerID = player.getUniqueId();
        inventoryChangeListener.saveInteraction.put(playerID,false);
        inventoryChangeListener.hangingInteraction.put(playerID,false);

        BukkitTask task = new BukkitRunnable() {
            public void run() {
                if (!player.isOnline()) {
                    this.cancel(); // 如果玩家不在线，则取消任务
                    return;
                }

                // 进行储存操作时暂时中止
                boolean save = inventoryChangeListener.saveInteraction.get(playerID) ||
                        inventoryChangeListener.hangingInteraction.get(playerID);
                if (save) return;

                // 创建或更新玩家的背包快照
                inventoryChangeListener.contrast(player);
            }
        }.runTaskTimer(this, 0L, snapshotIntervalTicks);

        playerSnapshotTasks.put(playerID, task); // 跟踪任务
    }

    public void updateSnapshotInterval() {
        // 取消所有现有任务
        playerSnapshotTasks.values().forEach(BukkitTask::cancel);
        playerSnapshotTasks.clear();

        // 对于每个在线玩家，重新启动快照更新任务
        Bukkit.getOnlinePlayers().forEach(this::updateSnapshot);
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
