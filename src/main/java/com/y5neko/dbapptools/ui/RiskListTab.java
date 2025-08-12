package com.y5neko.dbapptools.ui;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.y5neko.dbapptools.auth.AccountInfo;
import com.y5neko.dbapptools.auth.AccountStorage;
import com.y5neko.dbapptools.config.GlobalConfig;
import com.y5neko.dbapptools.network.HttpClientManager;
import com.y5neko.dbapptools.utils.LogUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class RiskListTab extends BorderPane {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DatePicker startDatePicker;
    private TextField startTimeField;
    private DatePicker endDatePicker;
    private TextField endTimeField;

    private Button queryBtn;
    private Button stopBtn;
    private TextArea responseArea;

    private volatile int totalCount = 0;
    private volatile int pendingRequests = 0;

    // 记录所有正在执行的请求
    private final List<Call> activeCalls = new CopyOnWriteArrayList<>();

    public RiskListTab() {
        initUI();
    }

    private void initUI() {
        Label startLabel = new Label("开始时间:");
        startDatePicker = new DatePicker(LocalDateTime.now().toLocalDate());
        startTimeField = new TextField("00:00:00");
        startTimeField.setPrefWidth(80);

        Label endLabel = new Label("结束时间:");
        endDatePicker = new DatePicker(LocalDateTime.now().toLocalDate());
        endTimeField = new TextField("23:59:59");
        endTimeField.setPrefWidth(80);

        HBox startBox = new HBox(5, startLabel, startDatePicker, startTimeField);
        startBox.setAlignment(Pos.CENTER_LEFT);

        HBox endBox = new HBox(5, endLabel, endDatePicker, endTimeField);
        endBox.setAlignment(Pos.CENTER_LEFT);

        queryBtn = new Button("查询");
        queryBtn.setOnAction(e -> sendRiskListRequest());

        stopBtn = new Button("停止");
        stopBtn.setDisable(true);
        stopBtn.setOnAction(e -> stopAllRequests());

        HBox buttonBox = new HBox(10, queryBtn, stopBtn);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        VBox controlBox = new VBox(10, startBox, endBox, buttonBox);
        controlBox.setPadding(new Insets(15));

        responseArea = new TextArea();
        responseArea.setEditable(false);
        responseArea.setWrapText(true);

        this.setTop(controlBox);
        this.setCenter(responseArea);
        this.setPadding(new Insets(10));
    }

    private void sendRiskListRequest() {
        responseArea.clear();
        queryBtn.setDisable(true);
        stopBtn.setDisable(false);
        totalCount = 0;
        activeCalls.clear();

        AccountInfo account = AccountStorage.loadAccount();
        if (account == null || account.getJwtToken() == null) {
            appendResponse("错误: 未找到有效JWT Token，请先登录获取");
            queryBtn.setDisable(false);
            stopBtn.setDisable(true);
            return;
        }
        String token = account.getJwtToken();
        String baseUrl = account.getLoginUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            appendResponse("错误: 登录地址为空，请先登录并填写地址");
            queryBtn.setDisable(false);
            stopBtn.setDisable(true);
            return;
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String url = baseUrl + GlobalConfig.RISK_LIST_URL;

        LocalDateTime startDateTime = parseDateTime(startDatePicker, startTimeField);
        LocalDateTime endDateTime = parseDateTime(endDatePicker, endTimeField);

        if (endDateTime.isBefore(startDateTime)) {
            appendResponse("错误: 结束时间不能早于开始时间");
            queryBtn.setDisable(false);
            stopBtn.setDisable(true);
            return;
        }

        List<TimeRange> timeRanges = splitTimeByHour(startDateTime, endDateTime);
        pendingRequests = timeRanges.size();

        appendResponse("拆分为 " + pendingRequests + " 个时间段，开始请求...");

        for (TimeRange range : timeRanges) {
            sendSingleRequest(url, token, range);
        }
    }

    private void appendResponse(String msg) {
        Platform.runLater(() -> responseArea.appendText(msg + "\n\n"));
    }

    private void sendSingleRequest(String url, String token, TimeRange range) {
        JSONObject jsonBody = buildRequestBody(range.start, range.end);
        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json;charset=UTF-8"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json;charset=UTF-8")
                .build();

        Call call = HttpClientManager.getInstance().newCall(request);
        activeCalls.add(call);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                if (call.isCanceled()) {
                    appendResponse("时间段 " + range.start + " ~ " + range.end + " 已取消");
                } else {
                    appendResponse("时间段 " + range.start + " ~ " + range.end + " 请求失败: " + e.getMessage());
                }
                onRequestFinished(call);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String respBody = response.body() != null ? response.body().string() : "";
                int total = 0;
                try {
                    JSONObject obj = JSON.parseObject(respBody);
                    JSONObject data = obj.getJSONObject("data");
                    total = data.getIntValue("total");
                } catch (Exception ignored) {}
                synchronized (RiskListTab.this) {
                    totalCount += total;
                }
                appendResponse("时间段 " + range.start + " ~ " + range.end + " total: " + total);
                onRequestFinished(call);
            }
        });
    }

    private void onRequestFinished(Call call) {
        activeCalls.remove(call);
        synchronized (this) {
            pendingRequests--;
            if (pendingRequests <= 0) {
                appendResponse("所有请求完成，累计 total = " + totalCount);
                Platform.runLater(() -> {
                    queryBtn.setDisable(false);
                    stopBtn.setDisable(true);
                });
            }
        }
    }

    private void stopAllRequests() {
        for (Call call : activeCalls) {
            call.cancel();
        }
        activeCalls.clear();
        pendingRequests = 0;
        appendResponse("已停止所有请求");
        queryBtn.setDisable(false);
        stopBtn.setDisable(true);
    }

    private JSONObject buildRequestBody(LocalDateTime start, LocalDateTime end) {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("limit", GlobalConfig.LIMIT);
        jsonBody.put("offset", 0);
        jsonBody.put("total", null);
        jsonBody.put("queryId", null);
        jsonBody.put("maxaccessid", null);
        jsonBody.put("assetChildNodes", new Object[0]);
        jsonBody.put("combined", 1);
        jsonBody.put("attackgrades", null);
        jsonBody.put("attackstatuss", null);
        jsonBody.put("original", null);
        jsonBody.put("accesssubtype", new Object[0]);
        jsonBody.put("flags", null);
        jsonBody.put("sips", new Object[0]);
        jsonBody.put("dips", new Object[0]);
        jsonBody.put("assetOrganize", null);
        jsonBody.put("apptypeids", new Object[0]);
        jsonBody.put("eventypes", new Object[0]);
        jsonBody.put("incidentids", new Object[0]);
        jsonBody.put("pstates", new int[]{0});
        jsonBody.put("poid", "");
        jsonBody.put("replycode", "");
        jsonBody.put("cve", "");
        jsonBody.put("ruleid", "");
        jsonBody.put("domain", "");
        jsonBody.put("cnnvd", "");
        jsonBody.put("pcapId", "");
        jsonBody.put("ioctagtypes", null);
        jsonBody.put("oobcontent", "");
        jsonBody.put("payload", "");
        jsonBody.put("timeAgo", "m0");
        jsonBody.put("attackerip", "");
        jsonBody.put("begin", start.format(DATE_TIME_FORMATTER));
        jsonBody.put("end", end.format(DATE_TIME_FORMATTER));
        jsonBody.put("nonflags", new Object[0]);
        jsonBody.put("fromtype", null);
        jsonBody.put("direction", null);
        return jsonBody;
    }

    private LocalDateTime parseDateTime(DatePicker datePicker, TextField timeField) {
        try {
            String date = datePicker.getValue().toString();
            String time = timeField.getText().trim();
            if (time.isEmpty()) time = "00:00:00";
            return LocalDateTime.parse(date + " " + time, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            LogUtils.error(RiskListTab.class,"parseDateTime error", e);
            return LocalDateTime.now();
        }
    }

    private List<TimeRange> splitTimeByHour(LocalDateTime start, LocalDateTime end) {
        List<TimeRange> list = new ArrayList<>();
        LocalDateTime curStart = start;
        while (curStart.isBefore(end)) {
            LocalDateTime curEnd = curStart.plusHours(1);
            if (curEnd.isAfter(end)) {
                curEnd = end;
            }
            list.add(new TimeRange(curStart, curEnd));
            curStart = curEnd;
        }
        return list;
    }

    private static class TimeRange {
        LocalDateTime start;
        LocalDateTime end;
        TimeRange(LocalDateTime s, LocalDateTime e) {
            this.start = s;
            this.end = e;
        }
    }
}
