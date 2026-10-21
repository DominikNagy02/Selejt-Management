module hu.unideb.inf.kliens {
    requires javafx.controls;
    requires javafx.fxml;


    opens hu.unideb.inf.kliens to javafx.fxml;
    exports hu.unideb.inf.kliens;
}