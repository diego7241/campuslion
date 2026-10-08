package com.loiane.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Flujo de integracion de CampusLion (APF1): Controlador + Servicio + Repositorio + base H2 reales,
 * con la aplicacion completa levantada en un puerto aleatorio. No requiere Docker.
 */
class CampusLionApiFlowIntegrationTest extends CampusLionApiTestSupport {

    private static final Pattern ID = Pattern.compile("\"_id\"\\s*:\\s*(\\d+)");
    private static final String API = "/api/courses";

    @Test
    @DisplayName("CI-01/02/06: crear, consultar, rechazar duplicado y eliminar un curso de punta a punta")
    void courseLifecycleThroughTheRealApi() {
        String name = "Docker para Principiantes";
        String json = courseJson(name, "Introduccion");

        ResponseEntity<String> created = send(HttpMethod.POST, API, json);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        Matcher matcher = ID.matcher(created.getBody());
        assertTrue(matcher.find(), "la respuesta debe incluir el _id del curso creado");
        String id = matcher.group(1);

        ResponseEntity<String> fetched = send(HttpMethod.GET, API + "/" + id, null);
        assertEquals(HttpStatus.OK, fetched.getStatusCode());
        assertNotNull(fetched.getBody());
        assertTrue(fetched.getBody().contains(name));

        ResponseEntity<String> duplicate = send(HttpMethod.POST, API, json);
        assertTrue(duplicate.getStatusCode().is4xxClientError(),
                "un nombre repetido debe rechazarse, pero devolvio " + duplicate.getStatusCode());

        ResponseEntity<String> deleted = send(HttpMethod.DELETE, API + "/" + id, null);
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());

        ResponseEntity<String> afterDelete = send(HttpMethod.GET, API + "/" + id, null);
        assertEquals(HttpStatus.NOT_FOUND, afterDelete.getStatusCode());
    }

    @Test
    @DisplayName("CI-05: actualizar un curso y comprobar que el cambio persiste")
    void updatedCourseIsPersisted() {
        ResponseEntity<String> created = send(HttpMethod.POST, API, courseJson("Python desde Cero", "Primeros pasos"));
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        Matcher matcher = ID.matcher(created.getBody());
        assertTrue(matcher.find());
        String id = matcher.group(1);

        ResponseEntity<String> updated = send(HttpMethod.PUT, API + "/" + id,
                courseJson("Python Intermedio", "Funciones y modulos"));
        assertEquals(HttpStatus.OK, updated.getStatusCode());

        ResponseEntity<String> fetched = send(HttpMethod.GET, API + "/" + id, null);
        assertEquals(HttpStatus.OK, fetched.getStatusCode());
        assertTrue(fetched.getBody().contains("Python Intermedio"));
        assertTrue(fetched.getBody().contains("Funciones y modulos"));
    }

    @Test
    @DisplayName("CI-04: el listado respeta el tamano de pagina solicitado")
    void listingIsPaginated() {
        ResponseEntity<String> page = send(HttpMethod.GET, API + "?page=0&pageSize=2", null);

        assertEquals(HttpStatus.OK, page.getStatusCode());
        assertNotNull(page.getBody());
        long coursesInPage = Pattern.compile("\"category\"").matcher(page.getBody()).results().count();
        assertEquals(2, coursesInPage);
        assertTrue(page.getBody().contains("\"totalElements\""));
    }
}
