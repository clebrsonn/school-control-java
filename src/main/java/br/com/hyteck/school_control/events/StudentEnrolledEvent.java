package br.com.hyteck.school_control.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

@Getter
public class StudentEnrolledEvent extends ApplicationEvent {

    private final String enrollmentId;
    private final String studentId;
    private final String classroomId;
    private final LocalDate enrollmentDate;

    public StudentEnrolledEvent(Object source, String enrollmentId, String studentId, String classroomId, LocalDate enrollmentDate) {
        super(source);
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.classroomId = classroomId;
        this.enrollmentDate = enrollmentDate;
    }
}
