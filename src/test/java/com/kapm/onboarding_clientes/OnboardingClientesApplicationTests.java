package com.kapm.onboarding_clientes;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.Mockito.mockStatic;

@SpringBootTest
@ActiveProfiles("test")
class OnboardingClientesApplicationTests {

	@Test
	void contextLoads() {
		// Falla si el contexto de Spring no logra levantarse con el perfil de pruebas
	}

	@Test
	void mainDelegaEnSpringApplication() {
		String[] args = {"--server.port=0"};
		try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
			OnboardingClientesApplication.main(args);
			spring.verify(() -> SpringApplication.run(OnboardingClientesApplication.class, args));
		}
	}

}
