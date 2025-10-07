package br.com.luizbrand.worklog.controller;

import br.com.luizbrand.worklog.dto.response.UserResponse;
import br.com.luizbrand.worklog.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public ResponseEntity<List<UserResponse>> findAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<UserResponse> findUserByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(userService.findByPublicId(publicId));
    }

    @PostMapping("/{publicId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactiveUserByPublicId(@PathVariable UUID publicId) {
        userService.deactiveUser(publicId);
        return ResponseEntity.noContent().build();
    }

}
