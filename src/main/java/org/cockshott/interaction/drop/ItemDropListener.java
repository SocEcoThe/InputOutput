package org.cockshott.interaction.drop;

import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.cockshott.InputOutputPlugin;


public class ItemDropListener implements Listener {
    private final InputOutputPlugin plugin;

    public ItemDropListener(InputOutputPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Item droppedItem = event.getItemDrop();
        plugin.addItemToCheck(new DropItem(event.getPlayer().getUniqueId(), droppedItem));
        System.out.println("丢弃了一些内容");
    }
}
