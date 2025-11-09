package br.com.furb.zerifynotification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("br.com.furb")
public class ZerifyNotificationApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZerifyNotificationApplication.class, args);
	}

}
