package br.com.hyteck.school_control.usecases.classroom;

import br.com.hyteck.school_control.exceptions.DuplicateResourceException;
import br.com.hyteck.school_control.models.classrooms.ClassRoom;
import br.com.hyteck.school_control.repositories.ClassroomRepository;
import br.com.hyteck.school_control.web.dtos.classroom.ClassRoomRequest;
import br.com.hyteck.school_control.web.dtos.classroom.ClassRoomResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateClassRoomTest {

    @Mock
    private ClassroomRepository classroomRepository;

    @InjectMocks
    private CreateClassRoom createClassRoomUseCase;

    private ClassRoomRequest classRoomRequest;
    private ClassRoom classRoom;

    @BeforeEach
    void setUp() {
        classRoomRequest = new ClassRoomRequest(
                "Math 101",
                "2023",
                LocalTime.of(9, 0),
                LocalTime.of(10, 30)
        );

        classRoom = ClassRoom.builder()
                .id("classId123")
                .name("Math 101")
                .year("2023")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .build();
    }

    @Test
    void execute_ShouldCreateAndReturnClassRoomResponse_WhenRequestIsValidAndNoDuplicates() {
        // Arrange
        when(classroomRepository.existsByNameAndYear(classRoomRequest.name(), classRoomRequest.schoolYear())).thenReturn(false);
        when(classroomRepository.save(any(ClassRoom.class))).thenReturn(classRoom);

        // Act
        ClassRoomResponse response = createClassRoomUseCase.execute(classRoomRequest);

        // Assert
        assertNotNull(response);
        assertEquals(classRoom.getId(), response.id());
        assertEquals(classRoomRequest.name(), response.name());
        assertEquals(classRoomRequest.schoolYear(), response.schoolYear());
        assertEquals(classRoomRequest.startTime(), response.startTime());
        assertEquals(classRoomRequest.endTime(), response.endTime());

        verify(classroomRepository).existsByNameAndYear(classRoomRequest.name(), classRoomRequest.schoolYear());
        verify(classroomRepository).save(any(ClassRoom.class));
    }

    @Test
    void execute_ShouldThrowDuplicateResourceException_WhenClassRoomAlreadyExists() {
        // Arrange
        when(classroomRepository.existsByNameAndYear(classRoomRequest.name(), classRoomRequest.schoolYear())).thenReturn(true);

        // Act & Assert
        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () -> {
            createClassRoomUseCase.execute(classRoomRequest);
        });

        assertEquals("Já existe uma turma com o nome 'Math 101' para o ano letivo 2023", exception.getMessage());

        verify(classroomRepository).existsByNameAndYear(classRoomRequest.name(), classRoomRequest.schoolYear());
        verify(classroomRepository, never()).save(any(ClassRoom.class));
    }

    @Test
    void execute_ShouldCorrectlyMapRequestToEntityForSaving() {
        // Arrange
        when(classroomRepository.existsByNameAndYear(anyString(), anyString())).thenReturn(false);
        when(classroomRepository.save(any(ClassRoom.class))).thenAnswer(invocation -> {
            ClassRoom savedClass = invocation.getArgument(0);
            // Assert properties of the entity being saved
            assertEquals(classRoomRequest.name(), savedClass.getName());
            assertEquals(classRoomRequest.schoolYear(), savedClass.getYear());
            assertEquals(classRoomRequest.startTime(), savedClass.getStartTime());
            assertEquals(classRoomRequest.endTime(), savedClass.getEndTime());
            // Simulate setting an ID upon saving
            savedClass.setId("newClassId");
            return savedClass;
        });

        // Act
        ClassRoomResponse response = createClassRoomUseCase.execute(classRoomRequest);

        // Assert
        assertNotNull(response);
        assertEquals("newClassId", response.id()); // Check if the ID from the saved entity is used
        verify(classroomRepository).save(any(ClassRoom.class));
    }
}
