package com.mocktalkback.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Profile("!test")  // test 제외
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
