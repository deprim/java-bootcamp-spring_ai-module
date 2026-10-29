package com.accenture.springai_bootcamp_demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite::memory:")
class SpringaiBootcampDemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
