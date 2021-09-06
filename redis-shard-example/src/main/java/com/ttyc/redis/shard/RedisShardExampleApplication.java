package com.ttyc.redis.shard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class RedisShardExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedisShardExampleApplication.class, args);
    }

}
