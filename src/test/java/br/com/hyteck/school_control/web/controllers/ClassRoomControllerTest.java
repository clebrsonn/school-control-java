package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.usecases.classroom.*;
import br.com.hyteck.school_control.usecases.enrollment.FindEnrollmentsByClassRoomId;
import br.com.hyteck.school_control.web.dtos.classroom.ClassRoomRequest;
import br.com.hyteck.school_control.web.dtos.classroom.ClassRoomResponse;
import br.com.hyteck.school_control.web.dtos.classroom.EnrollmentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;
import java.util.List;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(ClassRoomController.class)
class ClassRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private CreateClassRoom createClassRoomUseCase;
    @MockBean
    private FindClassRoomById findClassRoomByIdUseCase;
    @MockBean
    private FindClassRooms findAllClassRoomsUseCase;
    @MockBean
    private UpdateClassRoom updateClassRoomUseCase;
    @MockBean
    private DeleteClassRoom deleteClassRoomUseCase;
    @MockBean
    private FindEnrollmentsByClassRoomId findEnrollmentsByClassRoomIdUseCase;

    @Autowired
    private ObjectMapper objectMapper;

    private ClassRoomRequest classRoomRequest;
    private ClassRoomResponse classRoomResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        classRoomRequest = new ClassRoomRequest(
                "Math 101",
                "2023",
                LocalTime.of(9, 0),
                LocalTime.of(10, 30)
        );

        classRoomResponse = new ClassRoomResponse(
                "classId123",
                "Math 101",
                "2023",
                LocalTime.of(9, 0),
                LocalTime.of(10, 30)
        );
    }

    @Test
    void createClassRoom_ShouldReturnCreated_WhenRequestIsValid() throws Exception {
        // Arrange
        when(createClassRoomUseCase.execute(any(ClassRoomRequest.class))).thenReturn(classRoomResponse);

        // Act & Assert
        mockMvc.perform(post("/classrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(classRoomRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", "http://localhost/classrooms/" + classRoomResponse.id()))
                .andExpect(jsonPath("$.id").value(classRoomResponse.id()))
                .andExpect(jsonPath("$.name").value(classRoomResponse.name()));

        verify(createClassRoomUseCase).execute(any(ClassRoomRequest.class));
    }

    @Test
    void createClassRoom_ShouldReturnBadRequest_WhenRequestIsInvalid() throws Exception {
        // Arrange
        ClassRoomRequest invalidRequest = new ClassRoomRequest(
                null, // Invalid: name is blank
                "2023",
                LocalTime.of(9, 0),
                LocalTime.of(10, 30)
        );

        // Act & Assert
        mockMvc.perform(post("/classrooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getClassRoomById_ShouldReturnClassRoomResponse_WhenClassRoomExists() throws Exception {
        // Arrange
        String classRoomId = "classId123";
        when(findClassRoomByIdUseCase.execute(classRoomId)).thenReturn(Optional.of(classRoomResponse));

        // Act & Assert
        mockMvc.perform(get("/classrooms/{id}", classRoomId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(classRoomResponse.id()))
                .andExpect(jsonPath("$.name").value(classRoomResponse.name()));

        verify(findClassRoomByIdUseCase).execute(classRoomId);
    }

    @Test
    void getClassRoomById_ShouldReturnNotFound_WhenClassRoomDoesNotExist() throws Exception {
        // Arrange
        String classRoomId = "nonExistentId";
        when(findClassRoomByIdUseCase.execute(classRoomId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/classrooms/{id}", classRoomId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(findClassRoomByIdUseCase).execute(classRoomId);
    }

    @Test
    void getAllClassRooms_ShouldReturnPageOfClassRoomResponses() throws Exception {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<ClassRoomResponse> classRoomList = Collections.singletonList(classRoomResponse);
        Page<ClassRoomResponse> classRoomPage = new PageImpl<>(classRoomList, pageable, classRoomList.size());

        when(findAllClassRoomsUseCase.execute(any(Pageable.class))).thenReturn(classRoomPage);

        // Act & Assert
        mockMvc.perform(get("/classrooms")
                        .param("page", String.valueOf(pageable.getPageNumber()))
                        .param("size", String.valueOf(pageable.getPageSize()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(classRoomResponse.id()))
                .andExpect(jsonPath("$.totalElements").value(classRoomList.size()));

        verify(findAllClassRoomsUseCase).execute(any(Pageable.class));
    }

    @Test
    void updateClassRoom_ShouldReturnUpdatedClassRoomResponse_WhenRequestIsValid() throws Exception {
        // Arrange
        String classRoomId = "classId123";
        ClassRoomResponse updatedClassRoomResponse = new ClassRoomResponse(
                classRoomId,
                "Math 102", // Name updated
                "2024",     // Year updated
                classRoomRequest.startTime(),
                classRoomRequest.endTime()
        );
        when(updateClassRoomUseCase.execute(any(String.class), any(ClassRoomRequest.class))).thenReturn(updatedClassRoomResponse);

        // Act & Assert
        mockMvc.perform(put("/classrooms/{id}", classRoomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(classRoomRequest))) // requestDTO contains the update payload
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(classRoomId))
                .andExpect(jsonPath("$.name").value(updatedClassRoomResponse.name()))
                .andExpect(jsonPath("$.schoolYear").value(updatedClassRoomResponse.schoolYear()));

        verify(updateClassRoomUseCase).execute(any(String.class), any(ClassRoomRequest.class));
    }

    @Test
    void deleteClassRoom_ShouldReturnNoContent_WhenClassRoomExists() throws Exception {
        // Arrange
        String classRoomId = "classId123";
        doNothing().when(deleteClassRoomUseCase).execute(classRoomId);

        // Act & Assert
        mockMvc.perform(delete("/classrooms/{id}", classRoomId))
                .andExpect(status().isNoContent());

        verify(deleteClassRoomUseCase).execute(classRoomId);
    }

    @Test
    void getEnrollmentsByClassroomId_ShouldReturnPageOfEnrollmentResponses() throws Exception {
        // Arrange
        String classRoomId = "classId123";
        Pageable pageable = PageRequest.of(0, 10);
        EnrollmentResponse enrollmentResponse = new EnrollmentResponse( // Simplified EnrollmentResponse
                "enrollId123", "studId123", "John Doe", classRoomId, "Math 101", "2023",
                null, null, "ACTIVE"
        );
        List<EnrollmentResponse> enrollmentList = Collections.singletonList(enrollmentResponse);
        Page<EnrollmentResponse> enrollmentPage = new PageImpl<>(enrollmentList, pageable, enrollmentList.size());

        when(findEnrollmentsByClassRoomIdUseCase.execute(any(String.class), any(Pageable.class))).thenReturn(enrollmentPage);

        // Act & Assert
        mockMvc.perform(get("/classrooms/{classroomId}/enrollments", classRoomId)
                        .param("page", String.valueOf(pageable.getPageNumber()))
                        .param("size", String.valueOf(pageable.getPageSize()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(enrollmentResponse.id()))
                .andExpect(jsonPath("$.content[0].classRoomId").value(classRoomId))
                .andExpect(jsonPath("$.totalElements").value(enrollmentList.size()));

        verify(findEnrollmentsByClassRoomIdUseCase).execute(any(String.class), any(Pageable.class));
    }
}
