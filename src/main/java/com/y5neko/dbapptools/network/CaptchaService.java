package com.y5neko.dbapptools.network;

import com.alibaba.fastjson2.JSONObject;
import com.y5neko.dbapptools.config.GlobalConfig;
import com.y5neko.dbapptools.utils.LogUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class CaptchaService {

    private final String baseUrl;

    /**
     * 构造函数，传入登录地址
     */
    public CaptchaService(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            this.baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        } else {
            this.baseUrl = baseUrl;
        }
    }

    public interface CaptchaCallback {
        void onResult(String base64Image, String hash, String error);
    }

    /**
     * 动态请求验证码接口
     */
    public void fetchCaptcha(CaptchaCallback callback) {
        // 这里固定用这个路径和参数，如果需要可以改成动态传参数
        String captchaUrl = baseUrl + GlobalConfig.CAPTCHA_URL;

        Request request = new Request.Builder()
                .url(captchaUrl)
                .get()
                .build();

        HttpClientManager.getInstance().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                callback.onResult(null, null, e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onResult(null, null, "HTTP错误码：" + response.code());
                    LogUtils.error(CaptchaService.class, "获取验证码失败: " + response.code());
                    return;
                }
                String body = response.body().string();
                try {
                    JSONObject obj = JSONObject.parseObject(body);
                    int errorCode = obj.getIntValue("error_code");
                    if (errorCode == 200) {
                        JSONObject data = obj.getJSONObject("data");
                        String base64Img = data.getString("base64");
                        String hash = data.getString("hash");
                        callback.onResult(base64Img, hash, null);
                        LogUtils.info(CaptchaService.class, "成功获取验证码");
                    } else {
                        LogUtils.error(CaptchaService.class, "接口错误码: " + errorCode);
                        callback.onResult(null, null, "接口错误码: " + errorCode);
                    }
                } catch (Exception e) {
                    LogUtils.error(CaptchaService.class, "解析验证码响应异常: " + e.getMessage());
                    callback.onResult(null, null, "解析异常: " + e.getMessage());
                }
            }
        });
    }
}
