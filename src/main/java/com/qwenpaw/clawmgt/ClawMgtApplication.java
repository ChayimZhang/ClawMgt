package com.qwenpaw.clawmgt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ClawMgtApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClawMgtApplication.class, args);
    }
}
