package com.epam.rd.autocode.assessment.appliances;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class ApplianceStoreSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApplianceStoreSpringApplication.class, args);
    }

}
