package com.y5neko.dbapptools.ui;

import com.alibaba.fastjson2.JSONObject;
import com.y5neko.dbapptools.auth.AccountInfo;
import com.y5neko.dbapptools.auth.AccountStorage;
import com.y5neko.dbapptools.network.AuthService;
import com.y5neko.dbapptools.network.CaptchaService;
import com.y5neko.dbapptools.utils.AESCTRUtils;
import com.y5neko.dbapptools.utils.JwtUtils;
import com.y5neko.dbapptools.utils.LogUtils;
import com.y5neko.dbapptools.utils.MiscUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.Base64;

public class LoginTab extends BorderPane {

    private ImageView captchaImageView;
    private Label hashLabel;
    private Button fetchCaptchaBtn;

    private TextField loginUrlField;
    private TextField usernameField;
    private PasswordField passwordField;
    private TextField captchaInputField;
    private ComboBox<Integer> loginTypeCombo;
    private Button loginBtn;
    private TextArea logArea;

    private volatile String currentCaptchaHash = null;

    private CaptchaService captchaService;
    private AuthService authService;

    public LoginTab() {
        authService = new AuthService();

        initUI();

        // 加载保存的账号，填充UI
        AccountInfo savedAccount = AccountStorage.loadAccount();
        if (savedAccount != null && savedAccount.getLoginUrl() != null) {
            loginUrlField.setText(savedAccount.getLoginUrl());
            captchaService = new CaptchaService(savedAccount.getLoginUrl());
        } else {
            captchaService = new CaptchaService("https://127.0.0.1");  // 默认
        }

        // 加载验证码
        fetchCaptcha();

        if (savedAccount != null) {
            if (savedAccount.getLoginUrl() != null) {
                loginUrlField.setText(savedAccount.getLoginUrl());
            }
            if (savedAccount.getUsername() != null) {
                usernameField.setText(savedAccount.getUsername());
            }
            if (savedAccount.getPassword() != null) {
                passwordField.setText(savedAccount.getPassword());
            }
        }

        if (savedAccount != null && savedAccount.getJwtToken() != null) {
            logArea.setText("已登录，请前往漏洞列表页面检查是否有效，JWT为:\n" + savedAccount.getJwtToken() + "\n");
            JwtUtils.JwtInfo info = JwtUtils.parse(savedAccount.getJwtToken());
            appendLog("======================\n" +
                    "当前登录用户为: " + JwtUtils.getClaimAsString(info, "username").orElse("<none>") +
                    "\n过期状态: " + JwtUtils.isExpired(info));
        }
    }

    private void initUI() {
        // Captcha部分
        captchaImageView = new ImageView();
        captchaImageView.setFitWidth(200);
        captchaImageView.setFitHeight(80);
        captchaImageView.setPreserveRatio(true);
        captchaImageView.setSmooth(true);
        captchaImageView.setCache(true);
        captchaImageView.setStyle("-fx-border-color: gray; -fx-border-width: 1;");

        hashLabel = new Label("Hash: ");

        fetchCaptchaBtn = new Button("获取验证码");
        fetchCaptchaBtn.setOnAction(e -> {
            String url = loginUrlField.getText().trim();
            if (url.isEmpty()) {
                appendLog("请先填写登录地址");
                return;
            }
            captchaService = new CaptchaService(url);
            fetchCaptcha();
        });

        VBox captchaBox = new VBox(10, captchaImageView, hashLabel, fetchCaptchaBtn);
        captchaBox.setAlignment(Pos.CENTER);
        captchaBox.setPadding(new Insets(15));

        // 登录表单
        loginUrlField = new TextField();
        loginUrlField.setPromptText("登录地址");

        usernameField = new TextField();
        usernameField.setPromptText("用户名");

        passwordField = new PasswordField();
        passwordField.setPromptText("密码");

        captchaInputField = new TextField();
        captchaInputField.setPromptText("请输入验证码");

        loginTypeCombo = new ComboBox<>();
        loginTypeCombo.getItems().addAll(0, 1);
        loginTypeCombo.setValue(1);

        loginBtn = new Button("登录");
        loginBtn.setOnAction(e -> sendLoginRequest());

        GridPane loginGrid = new GridPane();
        loginGrid.setVgap(10);
        loginGrid.setHgap(10);
        loginGrid.setPadding(new Insets(15));

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(Region.USE_PREF_SIZE);
        col1.setHgrow(Priority.NEVER);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);

        loginGrid.getColumnConstraints().addAll(col1, col2);

        loginGrid.add(new Label("登录地址:"), 0, 0);
        loginGrid.add(loginUrlField, 1, 0);
        loginGrid.add(new Label("用户名:"), 0, 1);
        loginGrid.add(usernameField, 1, 1);
        loginGrid.add(new Label("密码:"), 0, 2);
        loginGrid.add(passwordField, 1, 2);
        loginGrid.add(new Label("验证码:"), 0, 3);
        loginGrid.add(captchaInputField, 1, 3);
        loginGrid.add(new Label("登录类型:"), 0, 4);
        loginGrid.add(loginTypeCombo, 1, 4);
        loginGrid.add(loginBtn, 1, 5);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefHeight(150);

        VBox rightBox = new VBox(10, loginGrid, logArea);
        rightBox.setPadding(new Insets(15));

        HBox mainBox = new HBox(20, captchaBox, rightBox);
        mainBox.setPadding(new Insets(20));

        this.setCenter(mainBox);
    }

    private void fetchCaptcha() {
        logArea.setText("");
        fetchCaptchaBtn.setDisable(true);
//        appendLog("开始请求验证码...");
        hashLabel.setText("Hash: ");
        captchaImageView.setImage(null);

        captchaService.fetchCaptcha((base64Img, hash, error) -> {
            if (error != null) {
                appendLog("验证码请求失败: " + error);
            } else {
                currentCaptchaHash = hash;
                showCaptchaImage(base64Img);
                setHashLabel(hash);
//                appendLog("验证码获取成功，Hash已更新");
            }
            fetchCaptchaBtn.setDisable(false);
        });
    }

    private void showCaptchaImage(String base64Img) {
        Platform.runLater(() -> {
            try {
                byte[] imgBytes = Base64.getDecoder().decode(base64Img);
                Image img = new Image(new java.io.ByteArrayInputStream(imgBytes));
                captchaImageView.setImage(img);
            } catch (Exception e) {
                appendLog("验证码图片显示失败: " + e.getMessage());
                LogUtils.error(LoginTab.class, "验证码图片显示失败: " + e.getMessage());
            }
        });
    }

    private void setHashLabel(String hash) {
        Platform.runLater(() -> hashLabel.setText("Hash: " + hash));
    }

    private void sendLoginRequest() {
        logArea.setText("");

        if (currentCaptchaHash == null) {
            appendLog("请先获取验证码，获取后才能登录");
            return;
        }

        String loginUrl = loginUrlField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String captcha = captchaInputField.getText().trim();
        int loginType = loginTypeCombo.getValue();

        if (loginUrl.isEmpty()) {
            appendLog("登录地址不能为空");
            return;
        }

        // ============================== 先保存账号信息（jwtToken待登录成功后更新）====================================
//        AccountInfo account = new AccountInfo(username, password, null, loginUrl);
//        AccountStorage.saveAccount(account);
//        appendLog("账号信息已保存");

        if (username.isEmpty() || password.isEmpty()) {
            appendLog("用户名和密码不能为空");
            return;
        }
        if (captcha.isEmpty()) {
            appendLog("验证码不能为空");
            return;
        }

        // ======================================== 密码加密 ===============================================
        String timestamp = MiscUtils.getTimestamp();
        String times = MiscUtils.getTimes(timestamp);
        String aesKey = MiscUtils.getAESKeyByTimestamp(timestamp);
        String encryptedPwdBase64 = AESCTRUtils.quickUse(password, aesKey, "DbappAPTLoginSpe");

        appendLog("发送登录请求...");
        loginBtn.setDisable(true);

        authService.loginWithUrl(loginUrl, username, encryptedPwdBase64, captcha, loginType, currentCaptchaHash, times, new AuthService.LoginCallback() {
            @Override
            public void onSuccess(String response) {
                appendLog("登录请求成功，响应内容: " + response);
                setLoginBtn(true);
                JSONObject jsonObject = JSONObject.parseObject(response);
                String jwtToken = jsonObject.getString("token");
                AccountInfo account = new AccountInfo(username, password, jwtToken, loginUrl);
                AccountStorage.saveAccount(account);
                appendLog("登录成功，JWT Token已保存");
            }

            @Override
            public void onFailure(String error) {
                appendLog("登录请求失败: " + error);
                setLoginBtn(true);
            }
        });
    }

    private void appendLog(String msg) {
        Platform.runLater(() -> logArea.appendText(msg + "\n"));
    }

    private void setLoginBtn(boolean enable) {
        Platform.runLater(() -> loginBtn.setDisable(!enable));
    }
}
