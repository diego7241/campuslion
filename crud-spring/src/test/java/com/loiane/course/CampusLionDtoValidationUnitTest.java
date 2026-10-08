package com.loiane.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.loiane.course.dto.CourseRequestDTO;
import com.loiane.course.dto.LessonDTO;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/**
 * Pruebas unitarias de los limites de validacion del nombre de curso (APF1, casos CU-03 y CU-04).
 * Usa el validador de Jakarta directamente: no hay contexto de Spring ni base de datos.
 */
class CampusLionDtoValidationUnitTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static Set<ConstraintViolation<CourseRequestDTO>> validate(String name) {
        List<LessonDTO> lessons = List.of(new LessonDTO(null, "Introduccion al curso", "dQw4w9WgXcQ"));
        return VALIDATOR.validate(new CourseRequestDTO(name, "Back-end", lessons));
    }

    @Test
    @DisplayName("CU-03: un nombre de exactamente 150 caracteres es valido")
    void nameWithMaximumLengthIsValid() {
        String name = "Spring ".repeat(21) + "abc";
        assertEquals(150, name.length());

        assertTrue(validate(name).isEmpty());
    }

    @Test
    @DisplayName("CU-04: un nombre de 4 caracteres (bajo el minimo de 5) se rechaza")
    void nameBelowMinimumLengthIsRejected() {
        Set<ConstraintViolation<CourseRequestDTO>> violations = validate("Java");

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }
}
