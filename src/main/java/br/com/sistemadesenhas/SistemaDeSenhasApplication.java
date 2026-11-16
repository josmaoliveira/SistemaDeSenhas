package br.com.sistemadesenhas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Ponto de entrada do sistema. {@code @ConfigurationPropertiesScan} carrega as classes da pasta config. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SistemaDeSenhasApplication {

	public static void main(String[] args) {
		SpringApplication.run(SistemaDeSenhasApplication.class, args);
	}

}
