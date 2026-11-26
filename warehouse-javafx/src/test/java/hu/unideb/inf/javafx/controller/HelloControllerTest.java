package hu.unideb.inf.javafx.controller;

import hu.unideb.inf.javafx.model.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class HelloControllerTest {

    @Test
    void onHelloButtonClickDoesNotThrowWhenModelIsNull() {
        HelloController controller = new HelloController();

        assertDoesNotThrow(controller::onHelloButtonClick);
    }

    @Test
    void onHelloButtonClickDoesNotThrowWhenModelIsSet() {
        HelloController controller = new HelloController();
        controller.setModel(new Model());

        assertDoesNotThrow(controller::onHelloButtonClick);
    }
}