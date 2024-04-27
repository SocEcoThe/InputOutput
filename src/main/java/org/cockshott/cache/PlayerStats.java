package org.cockshott.cache;

import lombok.Getter;
import lombok.Setter;

import java.sql.Date;
import java.time.LocalDate;

@Getter
public class PlayerStats {
    private final Long onlineTime; // 玩家在线时间
    private final Date joinDate; // 玩家的登录日期

    // 构造函数，初始化时没有参数，设置默认值
    public PlayerStats() {
        this.onlineTime = System.currentTimeMillis() / 1000; // 记录初始在线时间
        this.joinDate = Date.valueOf(LocalDate.now()); // 记录初始在线日期
    }
}

