package org.cockshott.interaction;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.cockshott.tools.ItemUtils;
import org.cockshott.cache.CacheManager;

public class PlayerInventoryListener implements Listener {
    private final CacheManager cacheManager;
    private JavaPlugin plugin;
    private Inventory lastClickInventory = null;

    public PlayerInventoryListener(CacheManager cacheManager, JavaPlugin plugin) {
        this.cacheManager = cacheManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        InventoryAction action = event.getAction();
        Player player = (Player) event.getWhoClicked();
        String playerId = player.getName();
        Inventory clickedInventory = event.getClickedInventory();
        ItemStack cursor = event.getCursor();
        ItemStack currentItem = event.getCurrentItem();
//        System.out.println(event.getView().getTopInventory().getType());

        if (clickedInventory == null) return;

        // 根据动作类型处理鼠标交互
        switch (action) {
            //检测放置时鼠标吸附的东西
            case PLACE_ALL:
            case PLACE_SOME:
                //如果两次都在同样的容器
                if (lastClickInventory.getType().equals(clickedInventory.getType())) break;
                //如果拿起时在玩家容器记录为“投入”
                if (lastClickInventory.getType() == InventoryType.PLAYER) {
                    cacheManager.recordInput(playerId, ItemUtils.getAmount(cursor), ItemUtils.getType(cursor));
                } else { //否则记录为产出
                    cacheManager.recordOutput(playerId, ItemUtils.getAmount(cursor), ItemUtils.getType(cursor));
                }
                break;
            case PLACE_ONE:
                //右键操作
                if (lastClickInventory.getType().equals(clickedInventory.getType())) break;
                //如果拿起时在玩家容器记录为“投入”
                if (lastClickInventory.getType() == InventoryType.PLAYER) {
                    cacheManager.recordInput(playerId, 1, ItemUtils.getType(cursor));
                } else { //否则记录为产出
                    cacheManager.recordOutput(playerId, 1, ItemUtils.getType(cursor));
                }
                break;
            case SWAP_WITH_CURSOR:
                //交换操作
                if (lastClickInventory.getType().equals(clickedInventory.getType())) break;

                //如果拿起时在玩家容器记录为“投入”
                if (lastClickInventory.getType() == InventoryType.PLAYER) {
                    cacheManager.recordInput(playerId, ItemUtils.getAmount(cursor), ItemUtils.getType(cursor));
                } else { //否则记录为产出
                    cacheManager.recordOutput(playerId, ItemUtils.getAmount(cursor), ItemUtils.getType(cursor));
                }

                lastClickInventory = clickedInventory;
                break;
            //检测拿起时槽位里的东西
            case PICKUP_ALL:
            case PICKUP_ONE:
            case PICKUP_SOME:
            case PICKUP_HALF:
                lastClickInventory = clickedInventory;
                break;
            case MOVE_TO_OTHER_INVENTORY:
                // 处理Shift+点击
                if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                    handleMoveToOtherInventoryAction(player, currentItem, plugin, event, (item) -> {
                        if (event.getClickedInventory() != null) {
                            if (event.getClickedInventory().getType() == InventoryType.PLAYER) {
                                // 从玩家背包到容器，记录为“投入”
                                cacheManager.recordInput(playerId, ItemUtils.getAmount(item), ItemUtils.getType(item));
                            } else if (event.getView().getBottomInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                                // 从容器到玩家背包，记录为“产出”
                                cacheManager.recordOutput(playerId, ItemUtils.getAmount(item), ItemUtils.getType(item));
                            }
                        }
                    });
                }
                break;
            case HOTBAR_SWAP:
            case HOTBAR_MOVE_AND_READD:
                ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
                handleMoveToOtherInventoryAction(player, currentItem, plugin, event, (item) -> {
                    if (clickedInventory != null && clickedInventory.getType() != InventoryType.PLAYER) {
                        if (hotbarItem == null) {
                            cacheManager.recordOutput(playerId, ItemUtils.getAmount(item), ItemUtils.getType(item));
                        } else if (currentItem.getType() == Material.AIR) {
                            cacheManager.recordInput(playerId, ItemUtils.getAmount(hotbarItem), ItemUtils.getType(hotbarItem));
                        } else {
                            cacheManager.recordOutput(playerId, ItemUtils.getAmount(item), ItemUtils.getType(item));
                            cacheManager.recordInput(playerId, ItemUtils.getAmount(hotbarItem), ItemUtils.getType(hotbarItem));
                        }
                    }
                });
                break;
            default:
                // 其他操作不处理
                break;
        }
    }


    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        // 在这里处理消费事件，可以选择记录下来或者简单忽略
        // 例如，记录消费操作作为特定类型的“产出”
        System.out.println(ItemUtils.getType(event.getItem()));
        System.out.println(ItemUtils.getAmount(event.getItem()));
        String regex = "potion-type=(\\w+:\\w+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(event.getItem().getItemMeta().toString());

        if (matcher.find()) {
            String potionType = matcher.group(1);
            System.out.println(potionType); // 输出 minecraft:long_water_breathing
        }
//        cacheManager.recordOutput(event.getPlayer().getUniqueId(), event.getItem());
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {
        // 处理装满水桶的逻辑
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        // 处理清空水桶的逻辑
        System.out.println("消耗了一桶" + event.getBucket());
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        // 玩家合成了物品，这里可以处理逻辑
        // 检查是否通过快捷键合成可以通过分析玩家物品栏的变化等来判断
        System.out.println("合成了" + event.getInventory());
    }


    public int countItemInPlayerInventory(Player player, String material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && ItemUtils.getType(item).equals(material)) {
                count += ItemUtils.getAmount(item);
            }
        }
        return count;
    }

    public void handleMoveToOtherInventoryAction(Player player, ItemStack itemStack, JavaPlugin plugin, InventoryClickEvent event, Consumer<ItemStack> onSuccessfulMove) {
        String material = ItemUtils.getType(itemStack);
        ItemStack currentItemSnapshot = itemStack.clone();

        if (itemStack.getType() == Material.AIR && material.contains("AIR")) {
            material = ItemUtils.getType(player.getInventory().getItem(event.getHotbarButton()));
            currentItemSnapshot = player.getInventory().getItem(event.getHotbarButton()).clone();
        }

//        InventoryHolder holder = event.getView().getTopInventory().getHolder();
//        if (holder instanceof Chest || holder instanceof DoubleChest){
//            onSuccessfulMove.accept(currentItemSnapshot);
//            return;
//        }

        // 操作前的物品数量
        int beforeCount = countItemInPlayerInventory(player, material);

        String finalMaterial = material;
        ItemStack finalCurrentItemSnapshot = currentItemSnapshot;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // 操作后的物品数量
            int afterCount = countItemInPlayerInventory(player, finalMaterial);
            // 如果物品数量不等，说明操作成功
            if (afterCount != beforeCount) {
                onSuccessfulMove.accept(finalCurrentItemSnapshot);
            }
        }, 1L); // 延迟1 tick以等待物品移动完成
    }


}

