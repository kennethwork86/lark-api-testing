package com.dtctest.larkapitesting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import top.dtc.common.core.DtcApplication;
import top.dtc.common.core.config.EnableScheduling;
import top.dtc.common.core.config.EnableWeb;

@EnableWeb
@DtcApplication
@EnableScheduling
public class LarkApiTestingApplication {

    public static void main(String[] args) {
        SpringApplication.run(LarkApiTestingApplication.class, args);
    }

}
