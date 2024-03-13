package org.cockshott.Command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.List;

public class UpdateConfigTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("updateconfig")) {
            if (args.length == 1) {
                return Arrays.asList("valid_blocks", "upload_interval", "snapshot_interval");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("valid_blocks")) {
                return Arrays.asList("add", "remove", "update");
            }
        }
        return null;
    }
}

