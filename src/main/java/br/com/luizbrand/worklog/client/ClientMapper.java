package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.AddressRequest;
import br.com.luizbrand.worklog.client.dto.AddressResponse;
import br.com.luizbrand.worklog.client.dto.BranchRequest;
import br.com.luizbrand.worklog.client.dto.BranchResponse;
import br.com.luizbrand.worklog.client.dto.BranchUpdateRequest;
import br.com.luizbrand.worklog.client.dto.ClientRequest;
import br.com.luizbrand.worklog.client.dto.ClientResponse;
import br.com.luizbrand.worklog.client.dto.ClientSummary;
import br.com.luizbrand.worklog.client.dto.ClientUpdateRequest;
import br.com.luizbrand.worklog.client.dto.ContactRequest;
import br.com.luizbrand.worklog.client.dto.ContactResponse;
import br.com.luizbrand.worklog.system.Systems;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Mapper(componentModel = "spring")
public abstract class ClientMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),// Ignora o ID, será gerado pelo banco
            @Mapping(target = "publicId", ignore = true),// Ignora, será gerado pelo @PrePersist
            @Mapping(target = "createdAt", ignore = true), // Ignora, será gerado pelo @CreationTimestamp
            @Mapping(target = "updatedAt", ignore = true), // Ignora, será gerado pelo @UpdateTimestamp
            @Mapping(target = "isEnabled", ignore = true), // Ignora, o default da entidade é true
            @Mapping(target = "systems", source = "systems"),
            @Mapping(target = "branches", source = "clientRequest.branches")
    })
    public abstract Client toClient(ClientRequest clientRequest, List<Systems> systems);

    @Mapping(source = "isEnabled", target = "enabled")
    public abstract ClientResponse toClientResponse(Client client);

    @Mapping(source = "isEnabled", target = "enabled")
    public abstract ClientSummary toSummary(Client client);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "clientUpdateRequest.enabled", target = "isEnabled")
    @Mapping(target = "branches", ignore = true) // Filiais têm sub-recurso próprio
    public abstract void updateClient(ClientUpdateRequest clientUpdateRequest, List<Systems> systems, @MappingTarget Client client);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "publicId", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "isEnabled", ignore = true),
            @Mapping(target = "client", ignore = true), // Preenchido em wireBackReferences
            @Mapping(target = "isMatriz", source = "isMatriz", defaultValue = "false")
    })
    // public porque o ClientBranchService também consome: passou a fazer parte da
    // superfície do mapper, não é mais detalhe interno de toClient.
    public abstract ClientBranch toClientBranch(BranchRequest branchRequest);

    /**
     * PATCH parcial da filial. {@code isMatriz} e {@code isEnabled} ficam de
     * fora de propósito: a troca de matriz precisa rebaixar a anterior (regra de
     * serviço) e a inativação tem endpoint próprio.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "publicId", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "isEnabled", ignore = true),
            @Mapping(target = "isMatriz", ignore = true),
            @Mapping(target = "client", ignore = true)
    })
    public abstract void updateBranch(BranchUpdateRequest branchUpdateRequest, @MappingTarget ClientBranch branch);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "publicId", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "isEnabled", ignore = true),
            @Mapping(target = "branch", ignore = true), // Preenchido em wireBackReferences
            @Mapping(target = "principal", source = "principal", defaultValue = "false")
    })
    protected abstract ClientContact toClientContact(ContactRequest contactRequest);

    protected abstract Address toAddress(AddressRequest addressRequest);

    @Mapping(source = "isEnabled", target = "enabled")
    public abstract BranchResponse toBranchResponse(ClientBranch branch);

    protected abstract ContactResponse toContactResponse(ClientContact contact);

    protected abstract AddressResponse toAddressResponse(Address address);

    /**
     * Cliente sem filiais devolve lista vazia, não null — o front não precisa
     * tratar os dois casos.
     */
    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    protected abstract List<BranchResponse> toBranchResponseList(List<ClientBranch> branches);

    /**
     * O MapStruct não preenche as referências de volta (branch.client e
     * contact.branch). Sem elas o insert falha, porque as FKs são NOT NULL.
     */
    @AfterMapping
    protected void wireBackReferences(@MappingTarget Client client) {
        if (client.getBranches() == null) {
            return;
        }
        client.getBranches().forEach(branch -> {
            branch.setClient(client);
            if (branch.getContatos() != null) {
                branch.getContatos().forEach(contato -> contato.setBranch(branch));
            }
        });
    }

    protected OffsetDateTime fromLocalDateTimeToOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atOffset(ZoneOffset.UTC);
    }

}
