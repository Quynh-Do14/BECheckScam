package com.example.checkscamv2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {MailSenderAutoConfiguration.class})
@EnableScheduling
@EnableAsync
public class Checkscamv2Application {

    public static void main(String[] args) {
        SpringApplication.run(Checkscamv2Application.class, args);
    }

}
