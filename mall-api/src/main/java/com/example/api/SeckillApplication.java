package com.example.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 秒杀系统聚合启动类
 */
@SpringBootApplication
// 1. 确保扫描所有模块下的 Spring 组件 (Service, Component, Controller 等)
@ComponentScan(basePackages = "com.example")
// 2. 确保扫描所有模块下的 MyBatis Mapper 接口
@MapperScan("com.example.**.mapper")
public class SeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
        
        // 打印一个小提示，确认虚拟线程是否环境就绪（Java 21 特性）
        System.out.println("=================================================");
        System.out.println("秒杀系统启动成功！");
        System.out.println("当前运行环境: Java " + System.getProperty("java.version"));
        System.out.println("提示: 请确保在 application.yml 中开启了 virtual.enabled: true");
        System.out.println("=================================================");
    }
}