package com.younglin.partnerMatching;

// [加入编程导航](https://www.code-nav.cn/) 深耕编程提升【两年半】、国内净值【最高】的编程社群、用心服务【20000+】求学者、帮你自学编程【不走弯路】

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动类
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
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

// https://github.com/liyupi