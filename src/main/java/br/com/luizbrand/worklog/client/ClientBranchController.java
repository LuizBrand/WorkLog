package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.BranchRequest;
import br.com.luizbrand.worklog.client.dto.BranchResponse;
import br.com.luizbrand.worklog.client.enums.StatusFiltro;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clients/{publicId}/branches")
public class ClientBranchController implements ClientBranchControllerDocs {

    private final ClientBranchService clientBranchService;

    public ClientBranchController(ClientBranchService clientBranchService) {
        this.clientBranchService = clientBranchService;
    }

    @GetMapping
    public ResponseEntity<List<BranchResponse>> findBranchesByClient(
            @PathVariable UUID publicId,
            @RequestParam(required = false) StatusFiltro status) {
        return ResponseEntity.ok(clientBranchService.findBranchesByClient(publicId, status));
    }

    @PostMapping
    public ResponseEntity<BranchResponse> createBranch(
            @PathVariable UUID publicId,
            @RequestBody @Valid BranchRequest branchRequest) {
        BranchResponse branchResponse = clientBranchService.createBranch(publicId, branchRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(branchResponse);
    }

}
