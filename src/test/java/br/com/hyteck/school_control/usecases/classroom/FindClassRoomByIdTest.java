package br.com.hyteck.school_control.usecases.classroom;

import br.com.hyteck.school_control.models.classrooms.ClassRoom;
import br.com.hyteck.school_control.repositories.ClassroomRepository;
import br.com.hyteck.school_control.web.dtos.classroom.ClassRoomResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindClassRoomByIdTest {

    @Mock
    private ClassroomRepository classroomRepository;

    @InjectMocks
    private FindClassRoomById findClassRoomByIdUseCase;

    private ClassRoom classRoom;
    private String classRoomId = "classId123";

    @BeforeEach
    void setUp() {
        classRoom = ClassRoom.builder()
                .id(classRoomId)
                .name("History 102")
                .year("2024")
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 30))
                .build();
    }

    @Test
    void execute_ShouldReturnClassRoomResponse_WhenClassRoomExists() {
        // Arrange
        when(classroomRepository.findById(classRoomId)).thenReturn(Optional.of(classRoom));

        // Act
        Optional<ClassRoomResponse> responseOptional = findClassRoomByIdUseCase.execute(classRoomId);

        // Assert
        assertTrue(responseOptional.isPresent(), "Response should be present");
        ClassRoomResponse response = responseOptional.get();
        assertEquals(classRoom.getId(), response.id());
        assertEquals(classRoom.getName(), response.name());
        assertEquals(classRoom.getYear(), response.schoolYear());
        assertEquals(classRoom.getStartTime(), response.startTime());
        assertEquals(classRoom.getEndTime(), response.endTime());

        verify(classroomRepository).findById(classRoomId);
    }

    @Test
    void execute_ShouldReturnEmptyOptional_WhenClassRoomDoesNotExist() {
        // Arrange
        String nonExistentId = "nonExistentId";
        when(classroomRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act
        Optional<ClassRoomResponse> responseOptional = findClassRoomByIdUseCase.execute(nonExistentId);

        // Assert
        assertTrue(responseOptional.isEmpty(), "Response should be empty");

        verify(classroomRepository).findById(nonExistentId);
    }
}
