package br.com.hyteck.school_control.usecases.classroom;

import br.com.hyteck.school_control.exceptions.DeletionNotAllowedException;
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.classrooms.ClassRoom;
import br.com.hyteck.school_control.repositories.ClassroomRepository;
import br.com.hyteck.school_control.repositories.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteClassRoomTest {

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private DeleteClassRoom deleteClassRoomUseCase;

    private ClassRoom classRoom;
    private String classRoomId = "classId123";

    @BeforeEach
    void setUp() {
        classRoom = ClassRoom.builder()
                .id(classRoomId)
                .name("Math 101")
                .year("2023")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .build();
    }

    @Test
    void execute_ShouldDeleteClassRoom_WhenClassRoomExistsAndNoEnrollments() {
        // Arrange
        // Making classRoom a spy to partially mock its behavior for validateDeletionPrerequisites
        // OR, more simply, mock the repositories it uses internally.
        // Here, we directly mock the repository calls that validateDeletionPrerequisites would trigger.
        when(classroomRepository.findById(classRoomId)).thenReturn(Optional.of(classRoom));
        // This is what classRoom.validateDeletionPrerequisites(enrollmentRepository) will check.
        when(enrollmentRepository.existsByClassroomId(classRoomId)).thenReturn(false);
        doNothing().when(classroomRepository).deleteById(classRoomId);


        // Act
        assertDoesNotThrow(() -> deleteClassRoomUseCase.execute(classRoomId));

        // Assert
        verify(classroomRepository).findById(classRoomId);
        verify(enrollmentRepository).existsByClassroomId(classRoomId); // verify the check done by validateDeletionPrerequisites
        verify(classroomRepository).deleteById(classRoomId);
    }

    @Test
    void execute_ShouldThrowResourceNotFoundException_WhenClassRoomDoesNotExist() {
        // Arrange
        when(classroomRepository.findById(classRoomId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            deleteClassRoomUseCase.execute(classRoomId);
        });

        assertEquals("Turma não encontrada com ID: " + classRoomId, exception.getMessage());
        verify(classroomRepository).findById(classRoomId);
        verify(enrollmentRepository, never()).existsByClassroomId(anyString());
        verify(classroomRepository, never()).deleteById(anyString());
    }

    @Test
    void execute_ShouldThrowDeletionNotAllowedException_WhenEnrollmentsExist() {
        // Arrange
        // We need to ensure classRoom.validateDeletionPrerequisites throws the exception.
        // This happens if enrollmentRepository.existsByClassroomId returns true.
        when(classroomRepository.findById(classRoomId)).thenReturn(Optional.of(classRoom));
        when(enrollmentRepository.existsByClassroomId(classRoomId)).thenReturn(true);


        // Act & Assert
        DeletionNotAllowedException exception = assertThrows(DeletionNotAllowedException.class, () -> {
            deleteClassRoomUseCase.execute(classRoomId);
        });

        String expectedMessage = "Não é possível excluir a turma '" + classRoom.getName() + "' (" + classRoom.getYear() + ") pois existem matrículas associadas.";
        assertEquals(expectedMessage, exception.getMessage());

        verify(classroomRepository).findById(classRoomId);
        verify(enrollmentRepository).existsByClassroomId(classRoomId);
        verify(classroomRepository, never()).deleteById(anyString());
    }


    // Test for the scenario where validateDeletionPrerequisites itself is mocked on a spy
    // This is an alternative way to test, especially if validateDeletionPrerequisites had more complex logic
    // not directly testable by mocking repository calls alone.
    @Test
    void execute_ShouldDeleteClassRoom_WhenUsingSpyAndValidationPasses() {
        // Arrange
        ClassRoom spiedClassRoom = spy(classRoom); // Create a spy of the classroom object
        when(classroomRepository.findById(classRoomId)).thenReturn(Optional.of(spiedClassRoom));
        // Explicitly tell the spy to do nothing when validateDeletionPrerequisites is called
        // This means we are assuming the validation logic within validateDeletionPrerequisites is correct
        // and here we are just testing the flow of DeleteClassRoom use case.
        // For this to work, validateDeletionPrerequisites must not be final.
        doNothing().when(spiedClassRoom).validateDeletionPrerequisites(enrollmentRepository);
        doNothing().when(classroomRepository).deleteById(classRoomId);

        // Act
        assertDoesNotThrow(() -> deleteClassRoomUseCase.execute(classRoomId));

        // Assert
        verify(classroomRepository).findById(classRoomId);
        verify(spiedClassRoom).validateDeletionPrerequisites(enrollmentRepository); // Verify the spied method was called
        verify(classroomRepository).deleteById(classRoomId);
    }


    @Test
    void execute_ShouldThrowExceptionFromValidation_WhenUsingSpyAndValidationFails() {
        // Arrange
        ClassRoom spiedClassRoom = spy(classRoom);
        when(classroomRepository.findById(classRoomId)).thenReturn(Optional.of(spiedClassRoom));
        String exceptionMessage = "Deletion prerequisite failed";
        // Make the spied method throw an exception
        doThrow(new DeletionNotAllowedException(exceptionMessage)).when(spiedClassRoom).validateDeletionPrerequisites(enrollmentRepository);

        // Act & Assert
        DeletionNotAllowedException exception = assertThrows(DeletionNotAllowedException.class, () -> {
            deleteClassRoomUseCase.execute(classRoomId);
        });

        assertEquals(exceptionMessage, exception.getMessage());

        verify(classroomRepository).findById(classRoomId);
        verify(spiedClassRoom).validateDeletionPrerequisites(enrollmentRepository);
        verify(classroomRepository, never()).deleteById(anyString());
    }
}
