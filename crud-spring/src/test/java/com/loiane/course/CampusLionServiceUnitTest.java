package com.loiane.course;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loiane.course.dto.CourseRequestDTO;
import com.loiane.course.dto.mapper.CourseMapper;
import com.loiane.course.enums.Status;
import com.loiane.exception.BusinessException;
import com.loiane.exception.RecordNotFoundException;

/**
 * Pruebas unitarias propias de CampusLion (APF1) sobre la capa de Servicio.
 * Mockito aisla el repositorio: no hay base de datos ni contexto de Spring.
 */
@ExtendWith(MockitoExtension.class)
class CampusLionServiceUnitTest {

    @Mock
    private CourseRepository courseRepository;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, new CourseMapper());
    }

    @Test
    @DisplayName("CU-02: crear un curso con nombre repetido lanza BusinessException y no guarda")
    void createRejectsDuplicateName() {
        CourseRequestDTO request = TestData.createValidCourseRequest();
        when(courseRepository.findByNameIgnoringRestriction(request.name()))
                .thenReturn(List.of(TestData.createValidCourse()));

        assertThrows(BusinessException.class, () -> courseService.create(request));

        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("CU-01: crear un curso valido lo guarda con estado Active")
    void createStoresCourseAsActive() {
        CourseRequestDTO request = TestData.createValidCourseRequest();
        when(courseRepository.findByNameIgnoringRestriction(request.name())).thenReturn(List.of());
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        courseService.create(request);

        ArgumentCaptor<Course> saved = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(saved.capture());
        assertEquals(Status.ACTIVE, saved.getValue().getStatus());
        assertEquals(request.name(), saved.getValue().getName());
    }

    @Test
    @DisplayName("CU-05: actualizar conservando el propio nombre no se considera duplicado")
    void updateAllowsKeepingOwnName() {
        Course existing = TestData.createValidCourse();
        CourseRequestDTO request = TestData.createValidCourseRequest();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(courseRepository.findByNameIgnoringRestriction(request.name())).thenReturn(List.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> courseService.update(1L, request));

        verify(courseRepository).save(existing);
    }

    @Test
    @DisplayName("CU-07: eliminar un curso inexistente lanza RecordNotFoundException")
    void deleteMissingCourseThrowsNotFound() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecordNotFoundException.class, () -> courseService.delete(99L));

        verify(courseRepository, never()).delete(any());
    }
}
