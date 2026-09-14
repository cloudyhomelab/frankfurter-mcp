package dev.frankfurter.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FrankfurterMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrankfurterMcpApplication.class, args);
    }
}
