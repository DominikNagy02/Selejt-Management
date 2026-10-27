package hu.unideb.inf.kliens.controller;

import hu.unideb.inf.kliens.model.Model;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HelloController
{

    private Model model;

    public void setModel(Model model) {
        this.model = model;
    }
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick()
    {
        welcomeText.setText("Welcome to JavaFX Application!");
        System.out.println(model.getItem().getName());
    }
}
