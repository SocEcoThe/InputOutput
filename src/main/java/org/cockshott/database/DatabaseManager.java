package org.cockshott.database;

import com.zjyl1994.minecraftplugin.multicurrency.MultiCurrencyPlugin;
import org.cockshott.cache.ItemOperation;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class DatabaseManager {
    // 实现与数据库的交互，包括同步数据到数据库
    private DataSource hikari;

    public void initialization(){
        hikari = MultiCurrencyPlugin.getInstance().getHikari();

        try (Connection connection = hikari.getConnection()) {
            // 定义创建新表格的SQL语句
            connection.setAutoCommit(false);
            String createTableSQL = "CREATE TABLE IF NOT EXISTS `input_output` (" +
                    "`id` INT AUTO_INCREMENT NOT NULL," +
                    "`player` VARCHAR(36) NOT NULL," +
                    "`action` VARCHAR(255) NOT NULL," +
                    "`item_name` VARCHAR(255) NOT NULL," +
                    "`quantity` INT NOT NULL," +
                    "PRIMARY KEY (`id`)," +
                    "UNIQUE KEY `unique_player_item_action` (`player`, `action`, `item_name`)," +
                    "INDEX `player_uuid_index` (`player`)"+
                    ") ENGINE=INNODB DEFAULT CHARSET=utf8 COMMENT='玩家操作记录表';";

            // 执行创建表格的SQL语句
            try (PreparedStatement preparedStatement = connection.prepareStatement(createTableSQL)) {
                preparedStatement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private <T> T execute(DatabaseOperation<T> operation) {
        try (Connection connection = hikari.getConnection()) {
            try {
                // 开始事务
                connection.setAutoCommit(false);
                // 执行传入的操作
                T result = operation.run(connection);
                System.out.println("执行成功");
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
        DatabaseOperation<Void> uploadTask = (connection) -> {
            // 获取数据库连接
            String sql = "INSERT INTO input_output (player, action, item_name, quantity) " +
                    "VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity);";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                for (ItemOperation op : operations) {
                    pstmt.setString(1, op.getPlayerName());
                    pstmt.setString(2, op.getAction());
                    pstmt.setString(3, op.getItemName());
                    pstmt.setInt(4, op.getQuantity());
                    pstmt.addBatch(); // 添加到批处理
                }
                pstmt.executeBatch(); // 执行批处理
            }

            return null; // 返回值
        };

        // 执行上传任务
        execute(uploadTask);
    }

}

