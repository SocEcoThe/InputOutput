package org.cockshott.cache;

import java.util.*;

public class CacheManager {
    // 玩家的物品操作记录，每个玩家ID映射到一个Map，该Map记录了物品类型及其投入/产出的数量
    private final List<ItemOperation> operations;

    public CacheManager(List<ItemOperation> operations) {
        this.operations = operations;
    }

    // 记录投入操作
    public synchronized void recordInput(String playerName, int amount,String itemName,Boolean isStore) {
        if (!isStore){
            operations.add(new ItemOperation(playerName,"input",itemName,amount));
        }else {
            operations.add(new ItemOperation(playerName,"storeInput",itemName,amount));
        }
    }

    // 记录产出操作
    public synchronized void recordOutput(String playerName, int amount,String itemName,Boolean isStore) {
        if (!isStore){
            operations.add(new ItemOperation(playerName,"output",itemName,amount));
        }else {
            operations.add(new ItemOperation(playerName,"storeOutput",itemName,amount));
        }
    }
}

