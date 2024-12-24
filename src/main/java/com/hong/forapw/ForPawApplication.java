package com.hong.forapw;

import com.hong.forapw.domain.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
public class ForPawApplication {

	public static void main(String[] args) {
		SpringApplication.run(ForPawApplication.class, args);
	}

	@Profile("local")
	@Bean
	CommandLineRunner localServerStart(PasswordEncoder passwordEncoder, UserRepository userRepository){
		return args -> {
			//userRepository.saveAll(Arrays.asList(
			//
			//));
		};
	}
}
