package hu.unideb.inf.javafx.controller;

import hu.unideb.inf.javafx.model.Model;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HelloController {

    private Model model;

    public void setModel(Model model) {
        this.model = model;
    }

    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        if (welcomeText != null) {
            welcomeText.setText("Welcome to JavaFX Application!");
        }
        if (model != null && model.getItem() != null) {
            System.out.println(model.getItem().getName());
        }
    }
}