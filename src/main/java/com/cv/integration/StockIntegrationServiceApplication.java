package com.cv.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.jms.MapMessage;
import javax.jms.Session;

@SpringBootApplication
@Slf4j
@EnableScheduling
public class StockIntegrationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockIntegrationServiceApplication.class, args);

    }

}
