package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.CnpjLookupResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clients")
public class ClientLookupController implements ClientLookupControllerDocs {

    private final CnpjLookupService cnpjLookupService;

    public ClientLookupController(CnpjLookupService cnpjLookupService) {
        this.cnpjLookupService = cnpjLookupService;
    }

    /**
     * O parâmetro é opcional na assinatura para que a ausência caia na mesma
     * validação de serviço da entrada inválida — e devolva o corpo de erro
     * padrão da API, não o 400 vazio do Spring.
     */
    @GetMapping("/lookup")
    public ResponseEntity<CnpjLookupResponse> lookupByCnpj(
            @RequestParam(required = false) String documento) {
        return ResponseEntity.ok(cnpjLookupService.lookup(documento));
    }
}
