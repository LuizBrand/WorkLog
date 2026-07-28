package br.com.luizbrand.worklog.support;

import br.com.luizbrand.worklog.client.dto.AddressRequest;
import br.com.luizbrand.worklog.client.dto.BranchRequest;
import br.com.luizbrand.worklog.client.dto.ClientRequest;
import br.com.luizbrand.worklog.client.dto.ContactRequest;
import br.com.luizbrand.worklog.client.enums.ClientType;
import br.com.luizbrand.worklog.client.enums.ContactType;
import br.com.luizbrand.worklog.client.enums.RegimeTributario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class ClientRequestTestBuilder {

    // Documentos válidos (checksum ok) usados como fixture nos testes.
    public static final String CNPJ_VALIDO = "12345678000195";
    public static final String CNPJ_ALFANUMERICO_VALIDO = "12ABC34501DE35";
    public static final String CPF_VALIDO = "11144477735";

    private ClientType tipo = ClientType.PJ;
    private String name = "Test Client";
    private String nomeFantasia = null;
    private RegimeTributario regimeTributario = null;
    private List<UUID> systemsPublicIds = new ArrayList<>();
    private List<BranchRequest> branches = new ArrayList<>(List.of(matriz(CNPJ_VALIDO)));
    private Boolean enabled = null;

    public static ClientRequestTestBuilder aClientRequest() {
        return new ClientRequestTestBuilder();
    }

    public static BranchRequest matriz(String documento) {
        return branch(documento, true);
    }

    public static BranchRequest filial(String documento) {
        return branch(documento, false);
    }

    public static BranchRequest branch(String documento, Boolean isMatriz) {
        return new BranchRequest(documento, isMatriz, null, null, null, null, new ArrayList<>());
    }

    public static AddressRequest anAddress() {
        return new AddressRequest("01310100", "Av. Paulista", "1000", "Sala 1",
                "Bela Vista", "São Paulo", "SP");
    }

    public static ContactRequest aContact(ContactType tipo, String valor) {
        return new ContactRequest(tipo, valor, null, null);
    }

    public static ContactRequest aPrincipalContact(ContactType tipo, String valor) {
        return new ContactRequest(tipo, valor, null, true);
    }

    public ClientRequestTestBuilder withTipo(ClientType tipo) {
        this.tipo = tipo;
        return this;
    }

    public ClientRequestTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ClientRequestTestBuilder withNomeFantasia(String nomeFantasia) {
        this.nomeFantasia = nomeFantasia;
        return this;
    }

    public ClientRequestTestBuilder withRegimeTributario(RegimeTributario regimeTributario) {
        this.regimeTributario = regimeTributario;
        return this;
    }

    public ClientRequestTestBuilder withSystems(List<UUID> systemsPublicIds) {
        this.systemsPublicIds = systemsPublicIds;
        return this;
    }

    public ClientRequestTestBuilder withSystem(UUID systemPublicId) {
        this.systemsPublicIds = List.of(systemPublicId);
        return this;
    }

    public ClientRequestTestBuilder withBranches(BranchRequest... branches) {
        this.branches = Arrays.asList(branches);
        return this;
    }

    public ClientRequestTestBuilder withBranches(List<BranchRequest> branches) {
        this.branches = branches;
        return this;
    }

    public ClientRequestTestBuilder withDocumento(String documento) {
        this.branches = new ArrayList<>(List.of(matriz(documento)));
        return this;
    }

    public ClientRequestTestBuilder enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public ClientRequest build() {
        return new ClientRequest(tipo, name, nomeFantasia, regimeTributario,
                systemsPublicIds, branches, enabled);
    }
}
