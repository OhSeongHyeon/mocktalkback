package com.mocktalkback;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;


@ActiveProfiles("test")
@SpringBootTest
@TestPropertySource(properties = "DEV_SERVER_PORT=0")
class MocktalkbackApplicationTests {

    /**
        .\gradlew clean build --refresh-dependencies
        .\gradlew test
        h2 디비 파서 해야됨, 개발디비에 붙이려면 flyway 지랄남
     */
	// @org.junit.jupiter.api.Test void contextLoads() {}

}
