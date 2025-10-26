package br.com.luizbrand.worklog.controller;

import br.com.luizbrand.worklog.dto.request.ClientRequest;
import br.com.luizbrand.worklog.dto.response.ClientResponse;
import br.com.luizbrand.worklog.dto.searchFilters.ClientFiltersParams;
import br.com.luizbrand.worklog.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping()
    public ResponseEntity<List<ClientResponse>> findAllClients(
            ClientFiltersParams filtersParams) {
        return ResponseEntity.ok(clientService.findAllClients(filtersParams));
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
    public ResponseEntity<ClientResponse> updateClient(@RequestBody @Valid ClientRequest clientRequest, @PathVariable UUID publicId) {
        return ResponseEntity.ok( clientService.updateClient(publicId, clientRequest));
    }

}
