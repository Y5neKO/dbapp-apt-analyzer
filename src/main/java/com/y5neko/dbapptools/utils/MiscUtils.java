package com.y5neko.dbapptools.utils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MiscUtils {
    /**
     * 获取当前时间戳
     */
    public static String getTimestamp() {
        long currentTimestamp = System.currentTimeMillis();
        return String.valueOf(currentTimestamp);
    }

    /**
     * 时间戳转时间字符串
     */
    public static String getTimes(String timestamp) {
        Date date = new Date(Long.parseLong(timestamp));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    /**
     * 从时间戳生成AES密钥
     */
    public static String getAESKeyByTimestamp(String timestamp) {
        Date date = new Date(Long.parseLong(timestamp));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyddHH-MM-mm");
        String input = sdf.format(date);
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void initDir(String[] dirs) {
        for (String dir : dirs) {
            File file = new File(dir);
            if (!file.exists()) {
                file.mkdirs();
            }
        }
    }
}
