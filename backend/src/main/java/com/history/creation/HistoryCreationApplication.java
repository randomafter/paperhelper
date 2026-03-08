package com.history.creation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.history.creation.mapper")
public class HistoryCreationApplication {

    public static void main(String[] args) {
        SpringApplication.run(HistoryCreationApplication.class, args);
    }
}
