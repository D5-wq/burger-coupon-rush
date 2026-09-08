package com.d5wq.burger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class BurgerCouponRushApplication {

    public static void main(String[] args) {
        SpringApplication.run(BurgerCouponRushApplication.class, args);
    }
}
