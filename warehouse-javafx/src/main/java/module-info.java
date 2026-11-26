module hu.unideb.inf.javafx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires hu.unideb.inf.core;

    opens hu.unideb.inf.javafx to javafx.fxml;
    opens hu.unideb.inf.javafx.controller to javafx.fxml;
    opens hu.unideb.inf.javafx.model to com.fasterxml.jackson.databind;

    exports hu.unideb.inf.javafx;
    exports hu.unideb.inf.javafx.controller;
    exports hu.unideb.inf.javafx.model;
}