package br.com.luizbrand.worklog.system;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/systems")
public class SystemController {

    private final SystemService systemService;

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    @GetMapping()
    public ResponseEntity<List<SystemResponse>> findAllSystems() {
        return ResponseEntity.ok(systemService.findAllSystems());
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<SystemResponse> findSystemByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(systemService.getSystemByPublicId(publicId));

    }

    @PostMapping("/")
    public ResponseEntity<SystemResponse> saveSystem(@RequestBody @Valid SystemRequest systemRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(systemService.createSystem(systemRequest));
    }

    @PatchMapping("/{publicId}")
    public ResponseEntity<SystemResponse> updateSystem(@RequestBody @Valid SystemRequest systemRequest, @PathVariable UUID publicId) {
        return ResponseEntity.ok(systemService.updateSystem(systemRequest, publicId));
    }


}
