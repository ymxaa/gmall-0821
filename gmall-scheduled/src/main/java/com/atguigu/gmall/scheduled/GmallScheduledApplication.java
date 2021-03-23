package com.atguigu.gmall.scheduled;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@MapperScan("com.atguigu.gmall.scheduled.mapper")
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class GmallScheduledApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmallScheduledApplication.class, args);
    }

}
