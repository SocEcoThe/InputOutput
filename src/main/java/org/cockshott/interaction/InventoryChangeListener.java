package org.cockshott.interaction;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryChangeListener implements Listener {
    private final Map<Player, Map<String, Integer>> playerSnapshots = new HashMap<>();
    private final InventorySnapshot inventorySnapshot;
    public List<String> validBlocks;
    public Boolean saveInteraction = false;
    private Boolean isSign = false;


    public InventoryChangeListener(CacheManager cacheManager,List<String> validBlocks) {
        this.inventorySnapshot = new InventorySnapshot(cacheManager);
        this.validBlocks = validBlocks;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 检查是否为右键点击
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        // 确认点击的是箱子
        Block clickedBlock = event.getClickedBlock();
        if (isNoValidBlock(clickedBlock,validBlocks)) return;

        Player player = event.getPlayer();
        saveInteraction = true;

        if (!clickedBlock.getType().name().toUpperCase().contains("CHEST")){
            saveContrast(player,true);
            return;
        }

        if (signCheck(clickedBlock)){
            saveContrast(player,true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Block closeBlock = event.getInventory().getLocation().getBlock();
        Player player = (Player) event.getPlayer();

        if (isNoValidBlock(closeBlock,validBlocks)) return;
        if (isSign) return;

        saveContrast(player,false);
        saveInteraction = false;
        isSign = false;
    }

    public Boolean signCheck(Block block){
        for (BlockFace face : BlockFace.values()) {
            if (!(face == BlockFace.NORTH || face == BlockFace.SOUTH)) continue;

            Block relative = block.getRelative(face);
            // 判断是否为告示牌
            if (relative.getType().name().contains("SIGN")){
                isSign = true;
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

    public void contrast(Player player){
        if (playerSnapshots.containsKey(player)) {
            // 比较之前和当前的背包状态
            Map<String, Integer> beforeSnapshot = playerSnapshots.get(player);
            Map<String, Integer> afterSnapshot = inventorySnapshot.takeSnapshot(player);
            inventorySnapshot.compareSnapshots(beforeSnapshot, afterSnapshot, player,saveInteraction);
            // 更新快照以便下一次比较
            playerSnapshots.put(player, afterSnapshot);
        }else {
            playerSnapshots.put(player,inventorySnapshot.takeSnapshot(player));
        }
    }

    public void saveContrast(Player player,Boolean isOpen){
        Map<String, Integer> beforeSnapshot = playerSnapshots.get(player);
        Map<String, Integer> afterSnapshot = inventorySnapshot.takeSnapshot(player);
        if (isOpen){
            inventorySnapshot.compareSnapshots(beforeSnapshot, afterSnapshot, player,!saveInteraction);
        }else {
            inventorySnapshot.compareSnapshots(beforeSnapshot, afterSnapshot, player,saveInteraction);
        }
        playerSnapshots.put(player, afterSnapshot);
    }
}

