package org.cockshott.cache;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ItemOperation {
    private String playerName;
    private String action; // "input" 或 "output"
    private String itemName;
    private int quantity;

    public ItemOperation(String playerName, String action, String itemName, int quantity) {
        this.playerName = playerName;
        this.action = action;
        this.itemName = itemName;
        this.quantity = quantity;
    }
}

