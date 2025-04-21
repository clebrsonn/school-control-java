package br.com.hyteck.school_control.web.controllers;

import br.com.hyteck.school_control.web.dtos.classroom.ClassRoomRequest;
import br.com.hyteck.school_control.web.dtos.classroom.ClassRoomResponse;
import br.com.hyteck.school_control.usecases.classroom.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/classrooms") // Endpoint para turmas
public class ClassRoomController {

    private final CreateClassRoom createClassRoom;
    private final FindClassRoomById findClassRoomById;
    private final FindClassRooms findAllClassRooms;
    private final UpdateClassRoom updateClassRoom;
    private final DeleteClassRoom deleteClassRoom;

    public ClassRoomController(CreateClassRoom createClassRoom,
                               FindClassRoomById findClassRoomById,
                               FindClassRooms findAllClassRooms,
                               UpdateClassRoom updateClassRoom,
                               DeleteClassRoom deleteClassRoom) {
        this.createClassRoom = createClassRoom;
        this.findClassRoomById = findClassRoomById;
        this.findAllClassRooms = findAllClassRooms;
        this.updateClassRoom = updateClassRoom;
        this.deleteClassRoom = deleteClassRoom;
    }

    // --- CREATE ---
    @PostMapping
    public ResponseEntity<ClassRoomResponse> createClassRoom(
            @Valid @RequestBody ClassRoomRequest requestDTO) {
        ClassRoomResponse createdClassRoom = createClassRoom.execute(requestDTO);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdClassRoom.id())
                .toUri();
        return ResponseEntity.created(location).body(createdClassRoom);
    }

    // --- READ (Single) ---
    @GetMapping("/{id}")
    public ResponseEntity<ClassRoomResponse> getClassRoomById(@PathVariable String id) {
        return findClassRoomById.execute(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- READ (Multiple / Paginated) ---
    @GetMapping
    public ResponseEntity<Page<ClassRoomResponse>> getAllClassRooms(
            @PageableDefault(size = 10, sort = "schoolYear,name") Pageable pageable) { // Ordenação padrão
        Page<ClassRoomResponse> classRoomPage = findAllClassRooms.execute(pageable);
        return ResponseEntity.ok(classRoomPage);
    }

    // --- UPDATE ---
    @PutMapping("/{id}")
    public ResponseEntity<ClassRoomResponse> updateClassRoom(
            @PathVariable String id,
            @Valid @RequestBody ClassRoomRequest requestDTO) {
        ClassRoomResponse updatedClassRoom = updateClassRoom.execute(id, requestDTO);
        return ResponseEntity.ok(updatedClassRoom);
    }

    // --- DELETE ---
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClassRoom(@PathVariable String id) {
        deleteClassRoom.execute(id);
    }
}