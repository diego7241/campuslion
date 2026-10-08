package com.loiane.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * Flujo de integracion de CampusLion (APF1): Controlador + Servicio + Repositorio + base H2 reales,
 * con la aplicacion completa levantada en un puerto aleatorio. No requiere Docker.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CampusLionApiFlowIntegrationTest {

    private static final Pattern ID = Pattern.compile("\"_id\"\\s*:\\s*(\\d+)");

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

    private String url(String path) {
        return "http://localhost:" + port + "/api/courses" + path;
    }

    private ResponseEntity<String> send(HttpMethod method, String path, String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange(url(path), method, new HttpEntity<>(json, headers), String.class);
    }

    private static String courseJson(String name) {
        return "{\"name\":\"" + name + "\",\"category\":\"Back-end\","
                + "\"lessons\":[{\"name\":\"Introduccion\",\"youtubeUrl\":\"dQw4w9WgXcQ\"}]}";
    }

    @Test
    @DisplayName("CI-01/02/06: crear, consultar, rechazar duplicado y eliminar un curso de punta a punta")
    void courseLifecycleThroughTheRealApi() {
        String name = "Docker para Principiantes";

        ResponseEntity<String> created = send(HttpMethod.POST, "", courseJson(name));
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        Matcher matcher = ID.matcher(created.getBody());
        assertTrue(matcher.find(), "la respuesta debe incluir el _id del curso creado");
        String id = matcher.group(1);

        ResponseEntity<String> fetched = send(HttpMethod.GET, "/" + id, null);
        assertEquals(HttpStatus.OK, fetched.getStatusCode());
        assertNotNull(fetched.getBody());
        assertTrue(fetched.getBody().contains(name));

        ResponseEntity<String> duplicate = send(HttpMethod.POST, "", courseJson(name));
        assertTrue(duplicate.getStatusCode().is4xxClientError(),
                "un nombre repetido debe rechazarse, pero devolvio " + duplicate.getStatusCode());

        ResponseEntity<String> deleted = send(HttpMethod.DELETE, "/" + id, null);
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());

        ResponseEntity<String> afterDelete = send(HttpMethod.GET, "/" + id, null);
        assertEquals(HttpStatus.NOT_FOUND, afterDelete.getStatusCode());
    }
}
