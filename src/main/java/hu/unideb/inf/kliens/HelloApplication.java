package hu.unideb.inf.kliens;

import hu.unideb.inf.kliens.controller.HelloController;
import hu.unideb.inf.kliens.model.Model;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application
{
    @Override
    public void start(Stage stage) throws IOException
    {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);

        ((HelloController)fxmlLoader.getController()).setModel(new Model());

        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }
}
