package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.ClientFiltersParams;
import br.com.luizbrand.worklog.client.dto.ClientRequest;
import br.com.luizbrand.worklog.client.dto.ClientResponse;
import br.com.luizbrand.worklog.client.dto.ClientUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/clients")
public class ClientController implements ClientControllerDocs {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    /**
     * Paginação opcional para não quebrar quem já consome a lista simples: sem
     * {@code page} nem {@code size} na query string a resposta continua sendo um
     * array de {@link ClientResponse}; com qualquer um dos dois, vira um
     * {@code Page}.
     */
    @GetMapping()
    public ResponseEntity<Object> findAllClients(
            ClientFiltersParams filtersParams,
            @PageableDefault(size = 12, sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {

        if (page == null && size == null) {
            return ResponseEntity.ok(clientService.findAllClients(filtersParams));
        }

        return ResponseEntity.ok(clientService.findAllClients(filtersParams, pageable));
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<ClientResponse> findClientByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(clientService.getClientByPublicId(publicId));
    }

    @PostMapping("/")
    public ResponseEntity<ClientResponse> saveClient(@RequestBody @Valid ClientRequest clientRequest) {
        ClientResponse clientResponse = clientService.createClient(clientRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(clientResponse);
    }

    @PatchMapping("/{publicId}")
    public ResponseEntity<ClientResponse> updateClient(@RequestBody @Valid ClientUpdateRequest clientUpdateRequest, @PathVariable UUID publicId) {
        return ResponseEntity.ok( clientService.updateClient(publicId, clientUpdateRequest));
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> softDeleteClient(@PathVariable UUID publicId) {
        clientService.softDeleteClient(publicId);
        return ResponseEntity.noContent().build();
    }

}
