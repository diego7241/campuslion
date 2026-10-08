package com.loiane.course;

import java.io.IOException;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * Base de las pruebas de integracion de CampusLion: levanta la aplicacion completa con H2 en un
 * puerto aleatorio (sin Docker) y ofrece un cliente HTTP que no lanza excepciones en errores 4xx/5xx.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class CampusLionApiTestSupport {

    @LocalServerPort
    private int port;

    private final RestTemplate http = restTemplate();

    private static RestTemplate restTemplate() {
        RestTemplate template = new RestTemplate();
        template.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return false;
            }
        });
        return template;
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    protected ResponseEntity<String> send(HttpMethod method, String path, String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange(baseUrl() + path, method, new HttpEntity<>(json, headers), String.class);
    }

    protected static String courseJson(String name, String lessonName) {
        return "{\"name\":\"" + name + "\",\"category\":\"Back-end\","
                + "\"lessons\":[{\"name\":\"" + lessonName + "\",\"youtubeUrl\":\"dQw4w9WgXcQ\"}]}";
    }
}
