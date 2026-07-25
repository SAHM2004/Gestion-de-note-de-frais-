package com.ids.expense;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ExpenseManagerApplicationTests {

	@Test
	void contextLoads() {
		System.out.println("BCRYPT HASH FOR password IS: " + new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("password"));
	}

}
