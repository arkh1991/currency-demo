package com.altbithunter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Главный класс сервиса.
 */
@SpringBootApplication
@EnableScheduling
public class CurrencyApplication {

    /**
     * Главный метод.
     *
     * @param args начальные параметры.
     */
    public static void main(final String[] args) {

        SpringApplication.run(CurrencyApplication.class, args);

    }

    /**
     * конструктор по умолчанию.
     */
    protected CurrencyApplication() {
    }
}
