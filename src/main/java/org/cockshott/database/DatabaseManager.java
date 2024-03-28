package org.cockshott.database;

import com.zjyl1994.minecraftplugin.multicurrency.MultiCurrencyPlugin;
import org.cockshott.cache.ItemOperation;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {
    // 实现与数据库的交互，包括同步数据到数据库
    private DataSource hikari;

    public void initialization(){
        hikari = MultiCurrencyPlugin.getInstance().getHikari();

        execute(connection -> {
            String[] create = {
                    "CREATE TABLE IF NOT EXISTS `user_table` ("+
                            "`id` INT AUTO_INCREMENT NOT NULL," +
                            "`player_uuid` VARCHAR(36) NOT NULL,"+
                            "`player_name` VARCHAR(255) NOT NULL,"+
                            "`online_time` INT NOT NULL DEFAULT 0,"+
                            "`action_date` DATE NOT NULL DEFAULT (CURDATE()),"+
                            "PRIMARY KEY (`id`),"+
                            "UNIQUE KEY `unique_player_date` (`player_uuid`, `action_date`),"+
                            "INDEX `player_uuid_index` (`player_uuid`)"+
                            ") ENGINE=INNODB DEFAULT CHARSET=utf8 COMMENT='玩家信息表';",
                    "CREATE TABLE IF NOT EXISTS `input_output` (" +
                            "`id` INT AUTO_INCREMENT NOT NULL," +
                            "`player_uuid` VARCHAR(36) NOT NULL," +
                            "`player_name` VARCHAR(255) NOT NULL," +
                            "`action` VARCHAR(255) NOT NULL," +
                            "`item_name` VARCHAR(255) NOT NULL," +
                            "`quantity` INT NOT NULL," +
                            "`action_time` DATE NOT NULL DEFAULT (CURDATE()),"+
                            "PRIMARY KEY (`id`)," +
                            "UNIQUE KEY `unique_player_item_action` (`player_uuid`, `action`, `item_name` ,`action_time`)," +
                            "INDEX `player_uuid_index` (`player_uuid`),"+
                            "FOREIGN KEY (`player_uuid`) REFERENCES `user_table`(`player_uuid`)"+
                            ") ENGINE=INNODB DEFAULT CHARSET=utf8 COMMENT='玩家操作记录表';"
            };
            // 执行创建表格的SQL语句
            for (String s : create) {
                PreparedStatement preparedStatement = connection.prepareStatement(s);
                preparedStatement.execute();
            }
            return null;
        });
    }

    private <T> T execute(DatabaseOperation<T> operation) {
        try (Connection connection = hikari.getConnection()) {
            try {
                // 开始事务
                connection.setAutoCommit(false);
                // 执行传入的操作
                T result = operation.run(connection);
                // 提交事务
                connection.commit();
                return result;
            } catch (Exception e) {
                // 出错时回滚事务
                connection.rollback();
                // 记录日志
                throw new RuntimeException("数据库操作失败", e);
            } finally {
                // 恢复自动提交设置
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            // 连接失败的异常处理
            throw new RuntimeException("获取数据库连接失败", e);
        }
    }

    public void uploadItemOperations(List<ItemOperation> operations) throws Exception {
        // 封装上传逻辑为 Callable 任务
        execute (connection -> {
            // 获取数据库连接
            String sql = "INSERT INTO input_output (player_uuid, player_name, action, item_name, quantity) " +
                    "VALUES (?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity);";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                for (ItemOperation op : operations) {
                    pstmt.setString(1, op.getPlayerUUID().toString());
                    pstmt.setString(2, op.getPlayerName());
                    pstmt.setString(3, op.getAction());
                    pstmt.setString(4, op.getItemName());
                    pstmt.setInt(5, op.getQuantity());
                    pstmt.addBatch(); // 添加到批处理
                }
                pstmt.executeBatch(); // 执行批处理
            }
            return null; // 返回值
        });
    }

    public void recordPlayerJoin(UUID playerId, String playerName) {
        execute(connection -> {
            // 检查玩家是否已存在于数据库
            String checkQuery = "SELECT COUNT(*) FROM user_table WHERE player_uuid = ? AND DATE(action_date) = CURDATE()";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                checkStmt.setString(1, playerId.toString());
                ResultSet resultSet = checkStmt.executeQuery();
                if (!(resultSet.next() && resultSet.getInt(1) > 0)) {
                    // 玩家当日记录不存在，插入新记录
                    String insertQuery = "INSERT INTO user_table (player_uuid, player_name, online_time) VALUES (?, ?, 0)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                        insertStmt.setString(1, playerId.toString());
                        insertStmt.setString(2, playerName);
                        insertStmt.executeUpdate();
                    }
                }
            }
            return null;
        });
    }

    public void recordPlayerQuit(UUID playerId, long onlineTime) {
        execute(connection -> {
            // 更新玩家在线时间
            String updateQuery = "UPDATE user_table SET online_time = online_time + ? WHERE player_uuid = ? AND DATE(action_date) = CURDATE()";
            try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                updateStmt.setLong(1, onlineTime);
                updateStmt.setString(2, playerId.toString());
                updateStmt.executeUpdate();
            }
            return null;
        });
    }

}

