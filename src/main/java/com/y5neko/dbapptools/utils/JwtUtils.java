package com.y5neko.dbapptools.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

/**
 * 简单的 JWT 解码/解析工具（只解码，不验证签名）
 */
public final class JwtUtils {

    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private JwtUtils() {}

    /**
     * 解析 JWT（支持 "Bearer ..." 前缀）。如果解析失败抛 IllegalArgumentException。
     */
    public static JwtInfo parse(String token) {
        Objects.requireNonNull(token, "token == null");

        token = token.trim();
        if (token.toLowerCase().startsWith("bearer ")) {
            token = token.substring(7).trim();
        }

        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("非法的 JWT 格式，期望 header.payload.signature");
        }

        String headerJson = decodeBase64UrlToString(parts[0]);
        String payloadJson = decodeBase64UrlToString(parts[1]);
        String signature = parts.length >= 3 ? parts[2] : "";

        JSONObject headerObj = safeParseJson(headerJson);
        JSONObject payloadObj = safeParseJson(payloadJson);

        return new JwtInfo(token, headerObj, payloadObj, signature);
    }

    /**
     * 仅解码单个段（Base64URL）为字符串
     */
    private static String decodeBase64UrlToString(String segment) {
        try {
            // Base64 URL decoder 在缺失 padding 时可能抛异常 —— 我们补充 '=' 再尝试
            byte[] decoded;
            try {
                decoded = URL_DECODER.decode(segment);
            } catch (IllegalArgumentException ex) {
                // 补 padding
                int mod = segment.length() % 4;
                if (mod != 0) {
                    int pad = 4 - mod;
                    segment = segment + repeat("=", pad);
                }
                decoded = URL_DECODER.decode(segment);
            }
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Base64URL 解码失败: " + ex.getMessage(), ex);
        }
    }

    private static JSONObject safeParseJson(String json) {
        try {
            return JSON.parseObject(json);
        } catch (Exception ex) {
            // 返回一个包含原始字符串的JSON对象，避免上层空指针
            JSONObject r = new JSONObject();
            r.put("_raw", json);
            return r;
        }
    }

    /**
     * 返回 token 中的指定 claim（如果存在），优先返回字符串形式
     */
    public static Optional<String> getClaimAsString(JwtInfo info, String claimKey) {
        if (info == null || info.payload == null) return Optional.empty();
        Object v = info.payload.get(claimKey);
        if (v == null) return Optional.empty();
        return Optional.of(String.valueOf(v));
    }

    /**
     * 是否已过期（基于 payload 中的 "exp" 字段）。
     * 支持 exp 单位为 秒 或 毫秒（通过大小判断）。
     * 如果没有 exp 字段，返回 false（视为无限期）。
     */
    public static boolean isExpired(JwtInfo info) {
        Instant exp = getExpirationInstant(info);
        if (exp == null) return false;
        return Instant.now().isAfter(exp);
    }

    /**
     * 获取 token 剩余秒数（可能为负）。如果没有 exp，返回 null。
     */
    public static Long getRemainingSeconds(JwtInfo info) {
        Instant exp = getExpirationInstant(info);
        if (exp == null) return null;
        return exp.getEpochSecond() - Instant.now().getEpochSecond();
    }

    /**
     * 解析 exp（如果存在），支持秒/毫秒
     */
    private static Instant getExpirationInstant(JwtInfo info) {
        if (info == null || info.payload == null) return null;
        Object expObj = info.payload.get("exp");
        if (expObj == null) return null;
        try {
            long expLong = 0L;
            if (expObj instanceof Number) {
                expLong = ((Number) expObj).longValue();
            } else {
                expLong = Long.parseLong(String.valueOf(expObj));
            }
            // 如果看起来像毫秒（> 1e12），转换成秒
            if (expLong > 1_000_000_000_000L) {
                return Instant.ofEpochMilli(expLong);
            } else {
                return Instant.ofEpochSecond(expLong);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 返回多行展示（header + payload + signature + exp 状态）
     */
    public static String prettyPrint(JwtInfo info) {
        if (info == null) return "null";

        StringBuilder sb = new StringBuilder();
        sb.append("Token: ").append(info.rawToken).append("\n\n");

        sb.append("Header:\n");
        if (info.header != null) sb.append(info.header.toJSONString()).append("\n\n");
        else sb.append("<none>\n\n");

        sb.append("Payload:\n");
        if (info.payload != null) sb.append(info.payload.toJSONString()).append("\n\n");
        else sb.append("<none>\n\n");

        sb.append("Signature:\n").append(info.signature == null ? "" : info.signature).append("\n\n");

        Instant exp = getExpirationInstant(info);
        if (exp != null) {
            sb.append("Expires at: ").append(DT_FMT.format(exp)).append("\n");
            long remaining = getRemainingSeconds(info);
            sb.append("Remaining seconds: ").append(remaining).append("\n");
            sb.append("Expired: ").append(isExpired(info)).append("\n");
        } else {
            sb.append("No exp claim present.\n");
        }
        return sb.toString();
    }

    /**
     * 包装解析结果
     */
    public static final class JwtInfo {
        public final String rawToken;
        public final JSONObject header;
        public final JSONObject payload;
        public final String signature;

        public JwtInfo(String rawToken, JSONObject header, JSONObject payload, String signature) {
            this.rawToken = rawToken;
            this.header = header;
            this.payload = payload;
            this.signature = signature;
        }
    }

    public static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        String token = "";

        JwtInfo info = parse(token);
        System.out.println(prettyPrint(info));

        System.out.println("username claim: " + getClaimAsString(info, "username").orElse("<none>"));
    }
}
