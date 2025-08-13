package com.yusufu.alticedemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AlticeDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlticeDemoApplication.class, args);
    }

}
