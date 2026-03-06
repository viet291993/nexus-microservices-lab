package com.nexus.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
@AutoConfigureWebTestClient
class ApiGatewayApplicationTests {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void testUnauthorizedRequest_ShouldReturn401() {
		// Gửi 1 request mà không đính kèm Token
		webTestClient.get()
				.uri("/api/v1/users/profile")
				.exchange()
				.expectStatus().isUnauthorized(); // Kỳ vọng Gateway chặn cửa
	}

	@Test
	void testAuthorizedRequest_ShouldReturn4xxWhenBackendNotMapped() {
		// Gửi 1 request CÓ Token giả lập (đã qua lớp bảo mật).
		// Route backend chưa map nên Gateway trả về 4xx (404 Not Found).
		webTestClient
				.get()
				.uri("/api/v1/users/profile")
				.header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
				.exchange()
				.expectStatus().is4xxClientError();
	}
}
