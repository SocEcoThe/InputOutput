package org.cockshott.tools;

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
}
