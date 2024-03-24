package org.cockshott.cache;

import org.bukkit.entity.Player;

import java.util.*;

public class CacheManager {
    // 玩家的物品操作记录，每个玩家ID映射到一个Map，该Map记录了物品类型及其投入/产出的数量
    private final List<ItemOperation> operations;

    public CacheManager(List<ItemOperation> operations) {
        this.operations = operations;
    }

    // 记录投入操作
    public synchronized void recordInput(Player player, int amount, String itemName, Boolean isStore) {
        if (isStore){
            operations.add(new ItemOperation(player.getUniqueId(), player.getName(), "storeInput",itemName,amount));
        }else {
            operations.add(new ItemOperation(player.getUniqueId(), player.getName(),"input",itemName,amount));
        }
    }

    // 记录产出操作
    public synchronized void recordOutput(Player player, int amount,String itemName,Boolean isStore) {
        if (isStore){
            operations.add(new ItemOperation(player.getUniqueId(), player.getName(),"storeOutput",itemName,amount));
        }else {
            operations.add(new ItemOperation(player.getUniqueId(), player.getName(),"output",itemName,amount));
        }
    }
}

