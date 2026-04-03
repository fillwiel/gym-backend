package com.wielkopolan.gymscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GymSchedulerApplication {

    static void main(String[] args) {
        SpringApplication.run(GymSchedulerApplication.class, args);
    }

}
