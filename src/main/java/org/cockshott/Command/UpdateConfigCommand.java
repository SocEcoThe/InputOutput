package org.cockshott.Command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.cockshott.InputOutputPlugin;

import java.util.Arrays;
import java.util.List;

public class UpdateConfigCommand implements CommandExecutor {

    private final InputOutputPlugin plugin;

    public UpdateConfigCommand(InputOutputPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("myplugin.updateconfig")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command.");
            return true;
        }
        // 检查命令参数长度
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /updateconfig <valid_blocks> <add/remove/update> <value(s)>");
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "valid_blocks":
                handleValidBlocks(sender, args);
                plugin.reloadValidBlocks();
                break;
            case "upload_interval":
                updateIntegerConfig(sender, args);
                plugin.reloadUploadTime();
            case "snapshot_interval":
                updateIntegerConfig(sender, args);
                plugin.reloadSnapshotTime();
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown setting: " + args[0]);
                return false;
        }

        // 保存配置更改
        plugin.saveConfig();
        return true;
    }

    private void handleValidBlocks(CommandSender sender, String[] args) {
        List<String> validBlocks = plugin.getConfig().getStringList("valid_blocks");

        switch (args[1].toLowerCase()) {
            case "add":
                // 添加单个有效方块
                if (args.length != 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /updateconfig valid_blocks add <block>");
                    return;
                }
                if (!validBlocks.contains(args[2])) {
                    validBlocks.add(args[2]);
                    sender.sendMessage(ChatColor.GREEN + "Added " + args[2] + " to valid_blocks.");
                } else {
                    sender.sendMessage(ChatColor.RED + args[2] + " is already in valid_blocks.");
                }
                break;
            case "remove":
                // 移除单个有效方块
                if (args.length != 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /updateconfig valid_blocks remove <block>");
                    return;
                }
                if (validBlocks.remove(args[2])) {
                    sender.sendMessage(ChatColor.GREEN + "Removed " + args[2] + " from valid_blocks.");
                } else {
                    sender.sendMessage(ChatColor.RED + args[2] + " was not found in valid_blocks.");
                }
                break;
            case "update":
                // 更新整个 valid_blocks 列表
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /updateconfig valid_blocks update <block1> <block2> ...");
                    return;
                }
                validBlocks.clear();
                validBlocks.addAll(Arrays.asList(args).subList(2, args.length));
                sender.sendMessage(ChatColor.GREEN + "Valid blocks updated.");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid operation for valid_blocks: " + args[1]);
                break;
        }

        plugin.getConfig().set("valid_blocks", validBlocks);
    }

    private void updateIntegerConfig(CommandSender sender, String[] args) {
        try {
            int newValue = Integer.parseInt(args[1]);
            plugin.getConfig().set(args[0], newValue);
            sender.sendMessage(ChatColor.GREEN + args[0] + " updated to " + newValue);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format for " + args[0]);
        }
    }
}

