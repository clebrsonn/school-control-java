package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.usecases.student.*;
import br.com.hyteck.school_control.web.dtos.student.StudentRequest;
import br.com.hyteck.school_control.web.dtos.student.StudentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.Collections;
import java.util.List;


@ExtendWith(SpringExtension.class)
@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private CreateStudent createStudentUseCase;
    @MockBean
    private FindStudentById findStudentByIdUseCase;
    @MockBean
    private FindStudents findAllStudentsUseCase;
    @MockBean
    private UpdateStudent updateStudentUseCase;
    @MockBean
    private DeleteStudent deleteStudentUseCase;

    @Autowired
    private ObjectMapper objectMapper;

    private StudentRequest studentRequest;
    private StudentResponse studentResponse;

    @BeforeEach
    void setUp() {
        // Re-initialize mockMvc before each test to ensure clean state, especially for security context
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();


        studentRequest = new StudentRequest(
                "John Doe",
                "john.doe@example.com",
                "12345678901", // CPF
                "respId123",
                "respPhone123",
                "classId123",
                "Class A",
                new BigDecimal("100.00"),
                new BigDecimal("500.00")
        );

        studentResponse = new StudentResponse(
                "studId123",
                "John Doe",
                "john.doe@example.com",
                "12345678901",
                "respId123",
                "Responsible Name",
                "Class A",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void createStudent_ShouldReturnCreated_WhenStudentRequestIsValid() throws Exception {
        // Arrange
        when(createStudentUseCase.execute(any(StudentRequest.class))).thenReturn(studentResponse);

        // Act & Assert
        mockMvc.perform(post("/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", "http://localhost/students/" + studentResponse.id()))
                .andExpect(jsonPath("$.id").value(studentResponse.id()))
                .andExpect(jsonPath("$.name").value(studentResponse.name()));

        verify(createStudentUseCase).execute(any(StudentRequest.class));
    }

    @Test
    void createStudent_ShouldReturnBadRequest_WhenStudentRequestIsInvalid() throws Exception {
        // Arrange
        StudentRequest invalidRequest = new StudentRequest(
                null, // Invalid: name is blank
                "john.doe@example.com",
                "12345678901",
                "respId123",
                "respPhone123",
                "classId123",
                "Class A",
                new BigDecimal("100.00"),
                new BigDecimal("500.00")
        );

        // Act & Assert
        // Note: We are not mocking createStudentUseCase here because validation should fail before it's called.
        // Spring Boot's @Valid annotation handles this.
        mockMvc.perform(post("/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }


    @Test
    void getStudentById_ShouldReturnStudentResponse_WhenStudentExists() throws Exception {
        // Arrange
        String studentId = "studId123";
        when(findStudentByIdUseCase.execute(studentId)).thenReturn(Optional.of(studentResponse));

        // Act & Assert
        mockMvc.perform(get("/students/{id}", studentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentResponse.id()))
                .andExpect(jsonPath("$.name").value(studentResponse.name()));

        verify(findStudentByIdUseCase).execute(studentId);
    }

    @Test
    void getStudentById_ShouldReturnNotFound_WhenStudentDoesNotExist() throws Exception {
        // Arrange
        String studentId = "nonExistentId";
        when(findStudentByIdUseCase.execute(studentId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/students/{id}", studentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(findStudentByIdUseCase).execute(studentId);
    }

    @Test
    void createStudentBulk_ShouldCallCreateStudentUseCaseForEachRequest() throws Exception {
        // Arrange
        List<StudentRequest> requestDTOs = List.of(studentRequest, studentRequest); // Two requests for simplicity
        // Assuming createStudentUseCase.execute doesn't return a value that needs to be checked here,
        // or it's complex to set up distinct returns for each call in this bulk scenario for a simple verification.
        // If it returned different IDs, more elaborate setup for studentResponse would be needed.
        when(createStudentUseCase.execute(any(StudentRequest.class))).thenReturn(studentResponse);


        // Act & Assert
        mockMvc.perform(post("/students/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTOs)))
                .andExpect(status().isOk()); // Assuming 200 OK for successful bulk void method

        verify(createStudentUseCase, times(requestDTOs.size())).execute(any(StudentRequest.class));
    }

    @Test
    void getAllStudents_ShouldReturnPageOfStudentResponses() throws Exception {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<StudentResponse> studentList = Collections.singletonList(studentResponse);
        PageImpl<StudentResponse> studentPage = new PageImpl<>(studentList, pageable, studentList.size());

        when(findAllStudentsUseCase.execute(any(Pageable.class))).thenReturn(studentPage);

        // Act & Assert
        mockMvc.perform(get("/students")
                        .param("page", String.valueOf(pageable.getPageNumber()))
                        .param("size", String.valueOf(pageable.getPageSize()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(studentResponse.id()))
                .andExpect(jsonPath("$.totalElements").value(studentList.size()));

        verify(findAllStudentsUseCase).execute(any(Pageable.class));
    }

    @Test
    void updateStudent_ShouldReturnUpdatedStudentResponse_WhenRequestIsValid() throws Exception {
        // Arrange
        String studentId = "studId123";
        StudentResponse updatedStudentResponse = new StudentResponse(
                studentId,
                "John Doe Updated", // Name updated
                studentRequest.email(),
                studentRequest.cpf(),
                studentRequest.responsibleId(),
                "Responsible Name Updated",
                studentRequest.className(),
                studentResponse.createdAt(), // createdAt usually doesn't change
                LocalDateTime.now() // updatedAt changes
        );
        when(updateStudentUseCase.execute(any(String.class), any(StudentRequest.class))).thenReturn(updatedStudentResponse);

        // Act & Assert
        mockMvc.perform(put("/students/{id}", studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentRequest))) // studentRequest contains the update payload
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentId))
                .andExpect(jsonPath("$.name").value(updatedStudentResponse.name()));

        verify(updateStudentUseCase).execute(any(String.class), any(StudentRequest.class));
    }

    @Test
    void deleteStudent_ShouldReturnNoContent_WhenStudentExists() throws Exception {
        // Arrange
        String studentId = "studId123";
        doNothing().when(deleteStudentUseCase).execute(studentId);

        // Act & Assert
        mockMvc.perform(delete("/students/{id}", studentId))
                .andExpect(status().isNoContent());

        verify(deleteStudentUseCase).execute(studentId);
    }
}
