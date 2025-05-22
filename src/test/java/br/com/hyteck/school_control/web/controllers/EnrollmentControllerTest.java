package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.usecases.enrollment.CreateEnrollment;
import br.com.hyteck.school_control.usecases.enrollment.FindEnrollmentsByStudentId;
import br.com.hyteck.school_control.web.dtos.classroom.EnrollmentRequest;
import br.com.hyteck.school_control.web.dtos.classroom.EnrollmentResponse;
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
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(EnrollmentController.class)
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private CreateEnrollment createEnrollmentUseCase;

    @MockBean
    private FindEnrollmentsByStudentId findEnrollmentsByStudentIdUseCase;

    @Autowired
    private ObjectMapper objectMapper;

    private EnrollmentRequest enrollmentRequest;
    private EnrollmentResponse enrollmentResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        enrollmentRequest = new EnrollmentRequest(
                "studId123",
                "classId123",
                "Math 101",
                new BigDecimal("50.00"),
                new BigDecimal("200.00")
        );

        enrollmentResponse = new EnrollmentResponse(
                "enrollId123",
                "studId123",
                "John Doe",
                "classId123",
                "Math 101",
                "2023",
                LocalDateTime.now(),
                null,
                "ACTIVE"
        );
    }

    @Test
    void enrollStudent_ShouldReturnCreated_WhenRequestIsValid() throws Exception {
        // Arrange
        when(createEnrollmentUseCase.execute(any(EnrollmentRequest.class))).thenReturn(enrollmentResponse);

        // Act & Assert
        mockMvc.perform(post("/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollmentRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", "http://localhost/enrollments/" + enrollmentResponse.id()))
                .andExpect(jsonPath("$.id").value(enrollmentResponse.id()))
                .andExpect(jsonPath("$.studentId").value(enrollmentResponse.studentId()));

        verify(createEnrollmentUseCase).execute(any(EnrollmentRequest.class));
    }

    @Test
    void enrollStudent_ShouldReturnBadRequest_WhenRequestIsInvalid() throws Exception {
        // Arrange
        EnrollmentRequest invalidRequest = new EnrollmentRequest(
                null, // Invalid: studentId is blank
                "classId123",
                "Math 101",
                new BigDecimal("50.00"),
                new BigDecimal("200.00")
        );

        // Act & Assert
        mockMvc.perform(post("/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStudentEnrollments_ShouldReturnEnrollmentList_WhenEnrollmentsExist() throws Exception {
        // Arrange
        String studentId = "studId123";
        List<EnrollmentResponse> enrollmentList = Collections.singletonList(enrollmentResponse);
        when(findEnrollmentsByStudentIdUseCase.execute(studentId)).thenReturn(enrollmentList);

        // Act & Assert
        mockMvc.perform(get("/enrollments/students/{studentId}", studentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(enrollmentResponse.id()))
                .andExpect(jsonPath("$[0].studentId").value(studentId));

        verify(findEnrollmentsByStudentIdUseCase).execute(studentId);
    }

    @Test
    void getStudentEnrollments_ShouldReturnNoContent_WhenNoEnrollmentsExist() throws Exception {
        // Arrange
        String studentId = "studIdWithNoEnrollments";
        when(findEnrollmentsByStudentIdUseCase.execute(studentId)).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/enrollments/students/{studentId}", studentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(findEnrollmentsByStudentIdUseCase).execute(studentId);
    }
}
