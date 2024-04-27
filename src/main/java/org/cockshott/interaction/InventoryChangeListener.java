package org.cockshott.interaction;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.cockshott.InputOutputPlugin;
import org.cockshott.cache.CacheManager;
import org.cockshott.tools.BlockTools;
import org.cockshott.tools.InventorySnapshot;

import java.util.*;

public class InventoryChangeListener implements Listener {
    private final Map<UUID, Map<String, Integer>> playerSnapshots = Collections.synchronizedMap(new HashMap<>());
    private final Map<UUID, Map<String, Integer>> playerPlace = Collections.synchronizedMap(new HashMap<>());
    private final InventorySnapshot inventorySnapshot;
    private final InputOutputPlugin plugin;
    public List<String> validBlocks;
    public final Map<UUID,Boolean> saveInteraction = new HashMap<>();
    public final Map<UUID,Boolean> hangingInteraction = new HashMap<>();


    public InventoryChangeListener(CacheManager cacheManager,List<String> validBlocks,InputOutputPlugin plugin) {
        this.inventorySnapshot = new InventorySnapshot(cacheManager);
        this.validBlocks = validBlocks;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        boolean isSneaking = event.getPlayer().isSneaking();
        boolean isLeftClickBlock = action == Action.LEFT_CLICK_BLOCK;
        boolean isRightClickBlock = event.getAction() == Action.RIGHT_CLICK_BLOCK;

        // 当不是右键和按住SHIFT+左键时退出
        if (!isRightClickBlock && !(isSneaking && isLeftClickBlock)) return;

        //当不在储存设置表中时退出
        Block clickedBlock = event.getClickedBlock();
        if (isNoValidBlock(clickedBlock)) return;

        Player player = event.getPlayer();
        UUID playerID = player.getUniqueId();
        hangingInteraction.put(playerID,true);

        new BukkitRunnable() {
            @Override
            public void run() {
                //如果容器未打开
                if(!saveInteraction.get(playerID)) {
                    saveContrast(player,true);
                }
                hangingInteraction.put(playerID,false);
            }
        }.runTaskLater(plugin, 5L);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Inventory openBlockInventory = event.getInventory();
        Player player = (Player) event.getPlayer();
        Block openBlock = getBlock(openBlockInventory,player);

        if (isNoValidBlock(openBlock)) return;

        saveInteraction.put(player.getUniqueId(),true);
        saveContrast(player,false);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory closeBlockInventory = event.getInventory();
        Player player = (Player) event.getPlayer();
        Block closeBlock = getBlock(closeBlockInventory,player);

        if (isNoValidBlock(closeBlock)) return;

        saveContrast(player,true);
        saveInteraction.put(player.getUniqueId(),false);

    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        // 获取事件的相关信息
        Block block = event.getBlockPlaced();
        UUID playerId = event.getPlayer().getUniqueId();
        String blockName = BlockTools.extractBlockType(block.toString());

        Map <String, Integer> blockCounts = playerPlace.getOrDefault(playerId, new HashMap<>());
        blockCounts.put(blockName, blockCounts.getOrDefault(blockName, 0) + 1);
        playerPlace.put(playerId, blockCounts);
    }

    public Block getBlock(Inventory inventory,Player player){
        Block block;
        Location blockLocation = inventory.getLocation();

        if (blockLocation != null){
            block = blockLocation.getBlock();
        }else {
            block = player.getTargetBlockExact(5);
        }

        return block;
    }

    private boolean isNoValidBlock(Block block) {
        if (block == null) {
            return true;
        }
        String blockName = BlockTools.extractBlockType(block.toString()).toUpperCase(); // 转换为大写以进行不区分大小写的比较
        // 检查 validBlocks 列表中是否没有任何一个字符串包含在 blockName 中
        return validBlocks.stream().noneMatch(validBlockName -> blockName.contains(validBlockName.toUpperCase()));
    }

    public synchronized void contrast(Player player){
        UUID playerID = player.getUniqueId();

        if (playerSnapshots.containsKey(playerID)) {
            // 比较之前和当前的背包状态
            Map<String, Integer> beforeSnapshot = playerSnapshots.get(playerID);
            Map<String, Integer> afterSnapshot = inventorySnapshot.takeSnapshot(player);
            inventorySnapshot.compareSnapshots(beforeSnapshot, afterSnapshot,playerPlace, player,false);
            // 更新快照以便下一次比较
            playerSnapshots.put(playerID, afterSnapshot);
        }else {
            playerSnapshots.put(playerID,inventorySnapshot.takeSnapshot(player));
        }
    }

    public synchronized void saveContrast(Player player,Boolean saveCheck){
        UUID playerID = player.getUniqueId();
        Map<String, Integer> beforeSnapshot = playerSnapshots.get(playerID);
        Map<String, Integer> afterSnapshot = inventorySnapshot.takeSnapshot(player);

        if (beforeSnapshot == null){
            beforeSnapshot = afterSnapshot;
        }

        inventorySnapshot.compareSnapshots(beforeSnapshot, afterSnapshot,playerPlace, player,saveCheck);
        playerSnapshots.put(playerID, afterSnapshot);
    }

    public void removePlayerSnapshots(UUID playerID){
        playerSnapshots.remove(playerID);
    }

    public void removeSaveInteraction(UUID playerID){
        playerSnapshots.remove(playerID);
    }

}

