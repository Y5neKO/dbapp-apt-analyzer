package com.y5neko.dbapptools.auth;

public class AccountInfo {
    private String username;
    private String password;
    private String jwtToken;
    private String loginUrl;

    public AccountInfo() {}

    public AccountInfo(String username, String password, String jwtToken, String loginUrl) {
        this.username = username;
        this.password = password;
        this.jwtToken = jwtToken;
        this.loginUrl = loginUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }
}
