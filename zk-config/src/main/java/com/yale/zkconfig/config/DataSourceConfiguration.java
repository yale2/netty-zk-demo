package com.yale.zkconfig.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yale
 */
@Configuration
public class DataSourceConfiguration  {

    @Bean
    public DruidDataSource dataSource(){
        DruidDataSource druidDataSource = new DruidDataSource();
        return druidDataSource;
    }

}
