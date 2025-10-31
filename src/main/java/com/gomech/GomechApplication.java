package com.gomech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GomechApplication {

        public static void main(String[] args) {
                SpringApplication.run(GomechApplication.class, args);
        }

}
