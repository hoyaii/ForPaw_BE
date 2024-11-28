package com.hong.forapw;

import com.hong.forapw.domain.District;
import com.hong.forapw.domain.Province;
import com.hong.forapw.domain.user.UserRole;
import com.hong.forapw.domain.user.User;
import com.hong.forapw.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

	@Autowired
	private PasswordEncoder passwordEncoder;

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

	private User newUser(String email, String name, String nickName, String password, UserRole userRole, String profileURL, Province province, District district, String subDistrict) {
		return User.builder()
				.email(email)
				.name(name)
				.nickName(nickName)
				.password(passwordEncoder.encode(password))
				.role(userRole)
				.profileURL(profileURL)
				.province(province)
				.district(district)
				.subDistrict(subDistrict)
				.build();
	}
}
