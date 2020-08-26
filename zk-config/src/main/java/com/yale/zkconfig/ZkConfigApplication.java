package com.yale.zkconfig;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author yale
 */
@SpringBootApplication
@MapperScan("com.yale.zkconfig.dao")
public class ZkConfigApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZkConfigApplication.class, args);
    }

}
