package com.y5neko.dbapptools;

import com.y5neko.dbapptools.ui.LoginTab;
import com.y5neko.dbapptools.config.GlobalConfig;
import com.y5neko.dbapptools.ui.RiskListTab;
import com.y5neko.dbapptools.utils.MiscUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class MainGUI extends Application {

    @Override
    public void start(Stage stage) {
        MiscUtils.initDir(GlobalConfig.DIR);

        TabPane tabPane = new TabPane();
        // ======================================== 登录页 ===============================================
        Tab loginTab = new Tab("登录");
        loginTab.setContent(new LoginTab());
        loginTab.setClosable(false);

        tabPane.getTabs().add(loginTab);
        // ======================================== 漏洞列表 ===============================================
        Tab riskListTab = new Tab("漏洞列表");
        riskListTab.setContent(new RiskListTab());
        riskListTab.setClosable(false);

        tabPane.getTabs().add(riskListTab);

        // ================================================================================================

        Scene scene = new Scene(tabPane, 800, 500);
        stage.setScene(scene);
        stage.setTitle("明御APT工具 by Y5neKO");

        stage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
