package com.y5neko.dbapptools.auth;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.y5neko.dbapptools.config.GlobalConfig;
import com.y5neko.dbapptools.utils.LogUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class AccountStorage {

    private static final File ACCOUNT_FILE = new File(GlobalConfig.ACCOUNT_FILE);

    public static void saveAccount(AccountInfo account) {
        try {
            JSONObject json = new JSONObject();
            json.put("username", account.getUsername());
            json.put("password", account.getPassword());
            json.put("jwtToken", account.getJwtToken());
            json.put("loginUrl", account.getLoginUrl());

            String jsonStr = json.toJSONString();
            if (!ACCOUNT_FILE.getParentFile().exists()) {
                ACCOUNT_FILE.getParentFile().mkdirs();
            }
            Files.write(ACCOUNT_FILE.toPath(), jsonStr.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LogUtils.error(AccountStorage.class, "保存账号信息失败: " + e.getMessage());
        }
    }

    public static AccountInfo loadAccount() {
        try {
            if (!ACCOUNT_FILE.exists()) {
                return null;
            }
            byte[] bytes = Files.readAllBytes(ACCOUNT_FILE.toPath());
            String jsonStr = new String(bytes);
            JSONObject json = JSON.parseObject(jsonStr);

            AccountInfo account = new AccountInfo();
            account.setUsername(json.getString("username"));
            account.setPassword(json.getString("password"));
            account.setJwtToken(json.getString("jwtToken"));
            account.setLoginUrl(json.getString("loginUrl"));

            return account;
        } catch (IOException e) {
            LogUtils.error(AccountStorage.class, "加载账号信息失败: " + e.getMessage());
            return null;
        }
    }
}
