package br.com.luizbrand.worklog.user;

import br.com.luizbrand.worklog.user.dto.ChangePasswordRequest;
import br.com.luizbrand.worklog.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController implements UserControllerDocs {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public ResponseEntity<List<UserResponse>> findAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.getMe(currentUser));
    }

    @PostMapping("/me/change-password")
    public ResponseEntity<Void> changeMyPassword(
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid ChangePasswordRequest request) {
        userService.changeMyPassword(currentUser, request);
        return ResponseEntity.noContent().build();
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
