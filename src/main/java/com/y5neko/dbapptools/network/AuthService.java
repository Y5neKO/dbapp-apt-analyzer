package com.y5neko.dbapptools.network;

import com.y5neko.dbapptools.config.GlobalConfig;
import com.y5neko.dbapptools.utils.LogUtils;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AuthService {

    private final OkHttpClient httpClient;

    public AuthService() {
        // 用跳过证书校验的 OkHttpClient
        httpClient = getUnsafeOkHttpClient()
                .newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public interface LoginCallback {
        void onSuccess(String response);
        void onFailure(String error);
    }

    // 动态传入登录地址
    public void loginWithUrl(String baseUrl, String username, String encryptedPassword, String captcha,
                             int loginType, String hash, String times, LoginCallback callback) {
        // 处理登录地址末尾是否有斜杠
        String loginUrl;
        if (baseUrl.endsWith("/")) {
            loginUrl = baseUrl.substring(0, baseUrl.length() - 1) + GlobalConfig.LOGIN_URL;
        } else {
            loginUrl = baseUrl + GlobalConfig.LOGIN_URL;
        }

        okhttp3.MediaType JSON = okhttp3.MediaType.get("application/json;charset=UTF-8");

        String jsonBody = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\",\"captcha\":\"%s\",\"loginType\":%d}",
                username, encryptedPassword, captcha, loginType);

        RequestBody body = RequestBody.create(jsonBody, JSON);

        Request request = new Request.Builder()
                .url(loginUrl)
                .post(body)
                .header("Authorization", "Bearer")
                .header("Encrypt", "true")
                .header("Hash", hash)
                .header("Times", times)
                .header("User-Type", "undefined")
                .header("Origin", baseUrl)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                callback.onFailure(e.getMessage());
                LogUtils.error(AuthService.class, "登录失败: " + e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.body() != null) {
                    callback.onSuccess(response.body().string());
                } else {
                    callback.onFailure("响应体为空");
                    LogUtils.error(AuthService.class, "登录失败: 响应体为空");
                }
            }
        });
    }

    private OkHttpClient getUnsafeOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[]{}; }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .build();
        } catch (Exception e) {
            LogUtils.error(AuthService.class, "创建不安全的 OkHttpClient 失败: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
