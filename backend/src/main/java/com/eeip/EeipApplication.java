package com.eeip;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@MapperScan("com.eeip.module.**.mapper")
public class EeipApplication {

    public static void main(String[] args) {
        SpringApplication.run(EeipApplication.class, args);
    }
}
