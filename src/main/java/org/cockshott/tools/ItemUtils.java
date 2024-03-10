package org.cockshott.tools;

import org.bukkit.inventory.ItemStack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemUtils {

    // 从ItemStack的toString结果中提取物品类型
    public static String getType(ItemStack item) {
        String str = item.toString();
        Pattern pattern = Pattern.compile("\\{([^ ]*?) x");
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return matcher.group(1); // 返回匹配到的物品类型字符串
        }
        return "Unknown"; // 如果没有匹配到，返回"Unknown"
    }

    // 从ItemStack的toString结果中提取物品数量
    public static int getAmount(ItemStack item) {
        String str = item.toString();
        Pattern pattern = Pattern.compile(" x (\\d+)");
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)); // 返回匹配到的数量字符串
        }
        return 0; // 如果没有匹配到，返回0
    }
}


