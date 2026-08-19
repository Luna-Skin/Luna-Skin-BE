package com.luna.skin;

import com.luna.skin.global.config.CorsProperties;
import com.luna.skin.global.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableConfigurationProperties({StorageProperties.class, CorsProperties.class})
public class LunaApplication {

	public static void main(String[] args) {
		SpringApplication.run(LunaApplication.class, args);
	}

}
