package org.cockshott.interaction;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.cockshott.cache.CacheManager;
import org.cockshott.tools.BlockTools;
import org.cockshott.tools.InventorySnapshot;

import java.util.*;

public class InventoryChangeListener implements Listener {
    private final Map<UUID, Map<String, Integer>> playerSnapshots = Collections.synchronizedMap(new HashMap<>());
    private final InventorySnapshot inventorySnapshot;
    public List<String> validBlocks;
    public final Map<UUID,Boolean> saveInteraction = Collections.synchronizedMap(new HashMap<>());


    public InventoryChangeListener(CacheManager cacheManager,List<String> validBlocks) {
        this.inventorySnapshot = new InventorySnapshot(cacheManager);
        this.validBlocks = validBlocks;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 检查是否为右键点击
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (isNoValidBlock(clickedBlock,validBlocks)) return;

        Player player = event.getPlayer();

        checkSpecialStorage(clickedBlock,player,"CHEST",true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Block closeBlock;
        Location closeBlockLocation = event.getInventory().getLocation();
        Player player = (Player) event.getPlayer();

        if (closeBlockLocation != null){
            closeBlock = closeBlockLocation.getBlock();
        }else {
            closeBlock = player.getTargetBlockExact(5);
        }

        if (isNoValidBlock(closeBlock,validBlocks)) return;

        checkSpecialStorage(closeBlock,player,"CHEST",false);

    }

    public Boolean signCheck(Block block){
        for (BlockFace face : BlockFace.values()) {
            if (!(face == BlockFace.NORTH || face == BlockFace.SOUTH)) continue;

            Block relative = block.getRelative(face);
            // 判断是否为告示牌
            if (relative.getType().name().contains("SIGN")){
                return true;
            }
        }
        return false;
    }

    private boolean isNoValidBlock(Block block, List<String> validBlocks) {
        if (block == null) {
            return true;
        }
        String blockName = BlockTools.extractBlockType(block.toString()).toUpperCase(); // 转换为大写以进行不区分大小写的比较

        // 检查 validBlocks 列表中是否没有任何一个字符串包含在 blockName 中
        return validBlocks.stream().noneMatch(validBlockName -> blockName.contains(validBlockName.toUpperCase()));
    }

    private void checkSpecialStorage(Block block,Player player,String specialStorageInteract,Boolean isOpen){
        boolean check = BlockTools.extractBlockType(block.toString()).toUpperCase().contains(specialStorageInteract);

        if (check && !signCheck(block)) {
            saveInteraction.put(player.getUniqueId(),false);
            return;
        }

        saveInteraction.put(player.getUniqueId(),isOpen);
        saveContrast(player);
    }

    public synchronized void contrast(Player player){
        UUID playerID = player.getUniqueId();
        if (!saveInteraction.containsKey(playerID)){
            saveInteraction.put(playerID,false);
        }

        if (playerSnapshots.containsKey(playerID)) {
            // 比较之前和当前的背包状态
            Map<String, Integer> beforeSnapshot = playerSnapshots.get(playerID);
            Map<String, Integer> afterSnapshot = inventorySnapshot.takeSnapshot(player);
            inventorySnapshot.compareSnapshots(beforeSnapshot, afterSnapshot, player,saveInteraction.get(playerID));
            // 更新快照以便下一次比较
            playerSnapshots.put(playerID, afterSnapshot);
        }else {
            playerSnapshots.put(playerID,inventorySnapshot.takeSnapshot(player));
        }
    }

    public synchronized void saveContrast(Player player){
        UUID playerID = player.getUniqueId();
        Map<String, Integer> beforeSnapshot = playerSnapshots.get(playerID);
        Map<String, Integer> afterSnapshot = inventorySnapshot.takeSnapshot(player);
        Boolean saveCheck = saveInteraction.get(playerID);

        if (saveCheck){
            inventorySnapshot.compareSnapshots(beforeSnapshot, afterSnapshot, player,saveCheck);
        }else {
            inventorySnapshot.compareSnapshots(beforeSnapshot, afterSnapshot, player,!saveCheck);
        }
        playerSnapshots.put(playerID, afterSnapshot);
    }

    public void removePlayerSnapshots(UUID playerID){
        playerSnapshots.remove(playerID);
    }

    public void removeSaveInteraction(UUID playerID){
        playerSnapshots.remove(playerID);
    }

}

