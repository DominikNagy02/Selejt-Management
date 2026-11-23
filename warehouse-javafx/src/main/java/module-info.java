module hu.unideb.inf.kliens {
    requires javafx.controls;
    requires javafx.fxml;


    opens hu.unideb.inf.kliens to javafx.fxml;
    exports hu.unideb.inf.kliens;
    exports hu.unideb.inf.kliens.controller;
    opens hu.unideb.inf.kliens.controller to javafx.fxml;
}