package br.com.hyteck.school_control.web.controllers;

// Removed ResourceNotFoundException, InvoiceItem, Types, InvoiceService, InvoiceItemUpdateRequestDto related imports
import br.com.hyteck.school_control.exceptions.ResourceNotFoundException;
import br.com.hyteck.school_control.models.classrooms.Enrollment;
import br.com.hyteck.school_control.models.classrooms.Student; // For building Enrollment
import br.com.hyteck.school_control.models.classrooms.ClassRoom; // For building Enrollment
import br.com.hyteck.school_control.usecases.enrollment.CreateEnrollment;
import br.com.hyteck.school_control.usecases.enrollment.FindEnrollmentsByStudentId;
import br.com.hyteck.school_control.usecases.enrollment.UpdateEnrollmentMonthlyFee; // Added
import br.com.hyteck.school_control.web.dtos.classroom.UpdateEnrollmentMonthlyFeeRequestDto; // Added
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test; // Added
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType; // Added
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal; // Added
import java.time.LocalDateTime; // Added

import static org.mockito.ArgumentMatchers.any; // Added
import static org.mockito.ArgumentMatchers.eq; // Added
import static org.mockito.Mockito.when; // Added
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch; // Added
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath; // Added
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status; // Added

@WebMvcTest(EnrollmentController.class)
public class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc; // Keep for other tests if any

    @Autowired
    private ObjectMapper objectMapper; // Keep for other tests if any

    @MockBean
    private CreateEnrollment createEnrollmentUseCase;

    @MockBean
    private FindEnrollmentsByStudentId findEnrollmentsByStudentId;

    @MockBean
    private UpdateEnrollmentMonthlyFee updateEnrollmentMonthlyFeeUseCase; // Added

    private final String existingEnrollmentId = "enroll-123";
    private final String nonExistentEnrollmentId = "enroll-404";

    @Test
    void shouldUpdateMonthlyFeeAndReturnOk() throws Exception {
        UpdateEnrollmentMonthlyFeeRequestDto requestDto = new UpdateEnrollmentMonthlyFeeRequestDto();
        BigDecimal newFee = new BigDecimal("550.75");
        requestDto.setMonthlyFee(newFee);

        Student student = Student.builder().id("student-001").name("John Doe").build();
        ClassRoom classRoom = ClassRoom.builder().id("class-101").name("Math 101").year("2024").build();

        Enrollment updatedEnrollment = Enrollment.builder()
                .id(existingEnrollmentId)
                .student(student)
                .classroom(classRoom)
                .monthlyFee(newFee)
                .startDate(LocalDateTime.now().minusDays(10))
                .status(Enrollment.Status.ACTIVE)
                .build();

        when(updateEnrollmentMonthlyFeeUseCase.execute(eq(existingEnrollmentId), eq(newFee)))
                .thenReturn(updatedEnrollment);

        mockMvc.perform(patch("/enrollments/{enrollmentId}/monthly-fee", existingEnrollmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingEnrollmentId))
                .andExpect(jsonPath("$.monthlyFee").value(newFee.doubleValue())) // Compare as double
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.classRoomId").value(classRoom.getId()))
                .andExpect(jsonPath("$.status").value(Enrollment.Status.ACTIVE.toString()));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMonthlyFeeForNonExistentEnrollment() throws Exception {
        UpdateEnrollmentMonthlyFeeRequestDto requestDto = new UpdateEnrollmentMonthlyFeeRequestDto();
        BigDecimal newFee = new BigDecimal("500.00");
        requestDto.setMonthlyFee(newFee);

        when(updateEnrollmentMonthlyFeeUseCase.execute(eq(nonExistentEnrollmentId), eq(newFee)))
                .thenThrow(new ResourceNotFoundException("Enrollment not found with id: " + nonExistentEnrollmentId));

        mockMvc.perform(patch("/enrollments/{enrollmentId}/monthly-fee", nonExistentEnrollmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestWhenMonthlyFeeIsNullInRequest() throws Exception {
        UpdateEnrollmentMonthlyFeeRequestDto requestDto = new UpdateEnrollmentMonthlyFeeRequestDto();
        requestDto.setMonthlyFee(null); // Invalid

        mockMvc.perform(patch("/enrollments/{enrollmentId}/monthly-fee", existingEnrollmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenMonthlyFeeIsNegativeInRequest() throws Exception {
        UpdateEnrollmentMonthlyFeeRequestDto requestDto = new UpdateEnrollmentMonthlyFeeRequestDto();
        requestDto.setMonthlyFee(new BigDecimal("-100.00")); // Invalid

        mockMvc.perform(patch("/enrollments/{enrollmentId}/monthly-fee", existingEnrollmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    // Test for PositiveOrZero: Zero should be allowed.
    @Test
    void shouldAllowZeroMonthlyFeeWhenUpdating() throws Exception {
        UpdateEnrollmentMonthlyFeeRequestDto requestDto = new UpdateEnrollmentMonthlyFeeRequestDto();
        BigDecimal zeroFee = BigDecimal.ZERO;
        requestDto.setMonthlyFee(zeroFee);

        Student student = Student.builder().id("student-002").name("Jane Zero").build();
        ClassRoom classRoom = ClassRoom.builder().id("class-102").name("Art 101").year("2024").build();


        Enrollment updatedEnrollmentWithZeroFee = Enrollment.builder()
                .id(existingEnrollmentId)
                .student(student)
                .classroom(classRoom)
                .monthlyFee(zeroFee)
                .startDate(LocalDateTime.now().minusMonths(1))
                .status(Enrollment.Status.ACTIVE)
                .build();

        when(updateEnrollmentMonthlyFeeUseCase.execute(eq(existingEnrollmentId), eq(zeroFee)))
                .thenReturn(updatedEnrollmentWithZeroFee);

        mockMvc.perform(patch("/enrollments/{enrollmentId}/monthly-fee", existingEnrollmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingEnrollmentId))
                .andExpect(jsonPath("$.monthlyFee").value(zeroFee.doubleValue()));
    }
}
