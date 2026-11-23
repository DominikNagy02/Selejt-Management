package hu.unideb.inf.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WarehouseBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(WarehouseBackendApplication.class, args);
    }
}
