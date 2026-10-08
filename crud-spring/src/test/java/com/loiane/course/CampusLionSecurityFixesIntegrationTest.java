package com.loiane.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Pruebas de regresion de las correcciones de seguridad de APF2 (INC-03, INC-04 e INC-06).
 */
class CampusLionSecurityFixesIntegrationTest extends CampusLionApiTestSupport {

    private static final String API = "/api/courses";

    @Test
    @DisplayName("INC-03: un nombre de leccion con HTML se rechaza con 400")
    void lessonNameWithHtmlIsRejected() {
        String json = courseJson("Curso Validacion Leccion", "<img src=x onerror=alert(1)>");

        ResponseEntity<String> response = send(HttpMethod.POST, API, json);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("INC-03: un nombre de leccion normal, con dos puntos y signos, sigue siendo valido")
    void regularLessonNameIsStillAccepted() {
        String json = courseJson("Curso Leccion Valida", "Introduccion: que es Spring Boot?");

        ResponseEntity<String> response = send(HttpMethod.POST, API, json);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @DisplayName("INC-04: la consola H2 no esta disponible")
    void h2ConsoleIsNotExposed() {
        ResponseEntity<String> response = send(HttpMethod.GET, "/h2-console", null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("INC-06: la API responde con las cabeceras de seguridad")
    void apiResponsesCarrySecurityHeaders() {
        ResponseEntity<String> response = send(HttpMethod.GET, API, null);

        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
        assertEquals("DENY", response.getHeaders().getFirst("X-Frame-Options"));
        assertNotNull(response.getHeaders().getFirst("Content-Security-Policy"));
    }
}
