package org.cockshott.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlockTools {

    public static String extractBlockType(String blockDescription) {
        String regex = "(?<=data\\=Block\\{)(.*?)(?=\\})";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(blockDescription);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return "Unknown";
    }

    public static void removeSuffix(List<String> list, String suffix) {
        for (int i = 0; i < list.size(); i++) {
            String element = list.get(i);
            // 检查元素是否以指定的后缀结尾
            if (element.endsWith(suffix)) {
                // 去掉结尾的后缀
                String newElement = element.substring(0, element.length() - suffix.length());
                // 更新列表中的元素
                list.set(i, newElement);
            }
        }
    }

    public static List<String> extractAndRemoveSuffix(List<String> originalList, String suffix) {
        // 创建新列表用于存储提取出来的元素
        List<String> newList = new ArrayList<>();
        for (String element : originalList) {
            // 检查元素是否以指定的后缀结尾
            if (element.endsWith(suffix)) {
                // 去掉结尾的后缀并添加到新列表中
                String newElement = element.substring(0, element.length() - suffix.length());
                newList.add(newElement);
            }
        }
        return newList;
    }
}
