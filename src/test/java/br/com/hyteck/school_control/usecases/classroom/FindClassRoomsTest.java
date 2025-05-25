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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindClassRoomsTest {

    @Mock
    private ClassroomRepository classroomRepository;

    @InjectMocks
    private FindClassRooms findClassRoomsUseCase;

    private ClassRoom classRoom1;
    private ClassRoom classRoom2;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        classRoom1 = ClassRoom.builder()
                .id("classId1")
                .name("Math 101")
                .year("2023")
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build();

        classRoom2 = ClassRoom.builder()
                .id("classId2")
                .name("History 101")
                .year("2023")
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .build();

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void execute_ShouldReturnPageOfClassRoomResponses_WhenClassRoomsExist() {
        // Arrange
        List<ClassRoom> classRoomList = List.of(classRoom1, classRoom2);
        Page<ClassRoom> classRoomPage = new PageImpl<>(classRoomList, pageable, classRoomList.size());
        when(classroomRepository.findAll(any(Pageable.class))).thenReturn(classRoomPage);

        // Act
        Page<ClassRoomResponse> responsePage = findClassRoomsUseCase.execute(pageable);

        // Assert
        assertNotNull(responsePage);
        assertEquals(2, responsePage.getTotalElements());
        assertEquals(2, responsePage.getContent().size());
        assertEquals(classRoom1.getId(), responsePage.getContent().get(0).id());
        assertEquals(classRoom1.getName(), responsePage.getContent().get(0).name());
        assertEquals(classRoom2.getId(), responsePage.getContent().get(1).id());
        assertEquals(classRoom2.getName(), responsePage.getContent().get(1).name());

        verify(classroomRepository).findAll(pageable);
    }

    @Test
    void execute_ShouldReturnEmptyPage_WhenNoClassRoomsExist() {
        // Arrange
        Page<ClassRoom> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(classroomRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        // Act
        Page<ClassRoomResponse> responsePage = findClassRoomsUseCase.execute(pageable);

        // Assert
        assertNotNull(responsePage);
        assertTrue(responsePage.isEmpty());
        assertEquals(0, responsePage.getTotalElements());

        verify(classroomRepository).findAll(pageable);
    }

    @Test
    void execute_ShouldCorrectlyMapClassRoomToClassRoomResponse() {
        // Arrange
        List<ClassRoom> classRoomList = List.of(classRoom1);
        Page<ClassRoom> classRoomPage = new PageImpl<>(classRoomList, pageable, classRoomList.size());
        when(classroomRepository.findAll(any(Pageable.class))).thenReturn(classRoomPage);

        // Act
        Page<ClassRoomResponse> responsePage = findClassRoomsUseCase.execute(pageable);

        // Assert
        assertNotNull(responsePage);
        assertFalse(responsePage.isEmpty());
        ClassRoomResponse singleResponse = responsePage.getContent().get(0);

        assertEquals(classRoom1.getId(), singleResponse.id());
        assertEquals(classRoom1.getName(), singleResponse.name());
        assertEquals(classRoom1.getYear(), singleResponse.schoolYear());
        assertEquals(classRoom1.getStartTime(), singleResponse.startTime());
        assertEquals(classRoom1.getEndTime(), singleResponse.endTime());

        verify(classroomRepository).findAll(pageable);
    }
}
