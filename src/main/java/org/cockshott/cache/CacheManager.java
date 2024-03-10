package org.cockshott.cache;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CacheManager {
    // 玩家的物品操作记录，每个玩家ID映射到一个Map，该Map记录了物品类型及其投入/产出的数量
    private final Map<UUID, Map<String, Integer>> playerTransactions = Collections.synchronizedMap(new HashMap<>());

    // 记录投入操作
    public synchronized void recordInput(UUID playerId, int amount,String material) {
        System.out.println("投入"+amount+"个"+material);
        playerTransactions.computeIfAbsent(playerId, k -> new HashMap<>()).merge(material, amount, Integer::sum);
    }

    // 记录产出操作
    public synchronized void recordOutput(UUID playerId, int amount,String material) {
        System.out.println("产出"+amount+"个"+material);
        playerTransactions.computeIfAbsent(playerId, k -> new HashMap<>()).merge(material, -amount, Integer::sum);
    }

    // 从缓存中获取一个玩家的所有记录
    public Map<String, Integer> getPlayerTransactions(UUID playerId) {
        return playerTransactions.getOrDefault(playerId, new HashMap<>());
    }

    //从缓存中删除一个玩家的所有记录
    public synchronized void removePlayerTransactions(UUID playerId) {
        playerTransactions.remove(playerId);
    }

    //获取所有玩家的记录
    public Map<UUID, Map<String, Integer>> getAllPlayerTransactions() {
        return new HashMap<>(playerTransactions);
    }
    
    
}

