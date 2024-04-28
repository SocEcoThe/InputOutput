package org.cockshott.cache;

import lombok.Getter;
import lombok.Setter;

import java.sql.Date;
import java.time.LocalDate;

@Getter
@Setter
public class PlayerStats {
    private long lastUpdateTime;
    private Date joinDate;
    private long onlineTime;

    public PlayerStats() {
        this.joinDate = Date.valueOf(LocalDate.now());
        this.lastUpdateTime = System.currentTimeMillis() / 1000; // 初始化为当前时间
        this.onlineTime = 0; // 初始化在线时间为0
    }
}

