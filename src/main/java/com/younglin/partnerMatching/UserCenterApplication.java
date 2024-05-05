package com.younglin.partnerMatching;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动类
 *
 * @author
 * @from
 */
@SpringBootApplication
@MapperScan("com.younglin.partnerMatching.mapper")
@Slf4j
@EnableScheduling
public class UserCenterApplication {

    public static void main(String[] args) {

        SpringApplication.run(UserCenterApplication.class, args);
        log.info("--启动成功--");
    }

}
