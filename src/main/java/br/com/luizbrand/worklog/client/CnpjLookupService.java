package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.AddressRequest;
import br.com.luizbrand.worklog.client.dto.BranchRequest;
import br.com.luizbrand.worklog.client.dto.CnpjLookupResponse;
import br.com.luizbrand.worklog.client.dto.ContactRequest;
import br.com.luizbrand.worklog.client.enums.ClientType;
import br.com.luizbrand.worklog.client.enums.ContactType;
import br.com.luizbrand.worklog.client.enums.RegimeTributario;
import br.com.luizbrand.worklog.client.integration.CnpjaCache;
import br.com.luizbrand.worklog.client.integration.CnpjaClient;
import br.com.luizbrand.worklog.client.integration.dto.CnpjaOfficeResponse;
import br.com.luizbrand.worklog.client.validation.DocumentoChecksum;
import br.com.luizbrand.worklog.exception.BadRequest.InvalidDocumentoException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Consulta de CNPJ para pré-preencher o formulário de cadastro.
 *
 * <p>Não persiste nada e não toca no {@code ClientService}: devolve os dados no
 * formato do formulário, o usuário revisa e só então salva pelo
 * {@code POST /clients}. Assim o caminho de escrita continua livre da latência
 * e da indisponibilidade de um serviço de terceiro.
 */
@Service
public class CnpjLookupService {

    private final CnpjaClient cnpjaClient;
    private final CnpjaCache cnpjaCache;

    public CnpjLookupService(CnpjaClient cnpjaClient, CnpjaCache cnpjaCache) {
        this.cnpjaClient = cnpjaClient;
        this.cnpjaCache = cnpjaCache;
    }

    public CnpjLookupResponse lookup(String documento) {
        String cnpj = BranchRequest.normalizeDocumento(documento);

        if (cnpj == null) {
            throw new InvalidDocumentoException("O CNPJ é obrigatório");
        }

        // Validar antes de sair para a rede não é preciosismo: a API pública
        // permite 5 consultas por minuto por IP, compartilhadas por toda a
        // equipe. Documento que já sabemos inválido não gasta uma delas.
        if (!DocumentoChecksum.isValid(ClientType.PJ, cnpj)) {
            throw new InvalidDocumentoException(DocumentoChecksum.mensagemPara(ClientType.PJ));
        }

        // O mapeamento é reaplicado mesmo em cache quente: o que se guarda é a
        // resposta crua do provedor, então mudar uma regra de tradução vale na
        // hora, sem esperar o TTL expirar.
        return toLookupResponse(cnpjaCache.get(cnpj).orElseGet(() -> consultarEGuardar(cnpj)));
    }

    /**
     * Só resposta bem-sucedida vai para o cache. Guardar um 404 esconderia por
     * 24h um CNPJ recém-aberto na Receita.
     */
    private CnpjaOfficeResponse consultarEGuardar(String cnpj) {
        CnpjaOfficeResponse office = cnpjaClient.findOffice(cnpj);
        cnpjaCache.put(cnpj, office);
        return office;
    }

    private CnpjLookupResponse toLookupResponse(CnpjaOfficeResponse office) {
        boolean matriz = Boolean.TRUE.equals(office.head());

        BranchRequest branch = new BranchRequest(
                office.taxId(),
                matriz,
                office.alias(),
                inscricaoEstadual(office),
                // Inscrição municipal não existe em nenhum ponto do contrato da
                // CNPJá: segue preenchimento manual.
                null,
                toAddress(office.address()),
                toContatos(office));

        return new CnpjLookupResponse(
                ClientType.PJ,
                office.company() == null ? null : office.company().name(),
                // A CNPJá tem um alias por estabelecimento; nosso modelo separa
                // nomeFantasia (empresa) de apelido (estabelecimento). Só a
                // matriz é a própria empresa, então só nela o alias vira
                // nomeFantasia — consultando uma filial, o usuário preenche.
                matriz ? office.alias() : null,
                regimeTributario(office.company()),
                office.status() == null ? null : office.status().text(),
                office.status() != null && Integer.valueOf(CnpjaOfficeResponse.Status.ATIVA).equals(office.status().id()),
                List.of(branch));
    }

    /**
     * Inscrição estadual do estabelecimento, que é a da <b>UF do endereço</b>.
     *
     * <p><b>Dormente na API pública.</b> Verificado contra a API real em
     * 2026-07-28: {@code open.cnpja.com} devolve {@code "registrations": null},
     * então na prática esta regra sempre cai no retorno nulo e a IE segue
     * preenchimento manual. O plano da fase afirmava o contrário; a afirmação
     * era falsa. O código fica porque a API comercial devolve o bloco — trocar
     * a {@code base-url} basta para ele passar a valer.
     *
     * <p>Quando o bloco existe, vem mais de uma inscrição: a IE é por
     * estabelecimento, e quem vende para outros estados se inscreve neles como
     * substituto tributário. A Ambev de Manaus tem quatro — pegar a primeira
     * traria a de Roraima. Dentro da UF, prefere IE Normal e habilitada.
     */
    private String inscricaoEstadual(CnpjaOfficeResponse office) {
        if (office.registrations() == null || office.address() == null
                || office.address().state() == null) {
            return null;
        }

        String uf = office.address().state();

        return office.registrations().stream()
                .filter(registration -> uf.equals(registration.state()))
                .min(Comparator
                        .comparing((CnpjaOfficeResponse.Registration registration) -> !isIeNormal(registration))
                        .thenComparing(registration -> !Boolean.TRUE.equals(registration.enabled())))
                .map(CnpjaOfficeResponse.Registration::number)
                .orElse(null);
    }

    private boolean isIeNormal(CnpjaOfficeResponse.Registration registration) {
        return registration.type() != null
                && Integer.valueOf(CnpjaOfficeResponse.Registration.Type.IE_NORMAL)
                .equals(registration.type().id());
    }

    /**
     * Só o que a API afirma. Lucro Presumido e Lucro Real não aparecem em ponto
     * nenhum do contrato, então ficam em branco em vez de serem adivinhados.
     */
    private RegimeTributario regimeTributario(CnpjaOfficeResponse.Company company) {
        if (company == null) {
            return null;
        }

        // MEI é checado ANTES do Simples de propósito: todo MEI é optante do
        // Simples, então a CNPJá devolve simples.optant = true para os dois. Na
        // ordem inversa, todo MEI seria cadastrado como SIMPLES_NACIONAL sem
        // erro nenhum aparecer.
        if (isOptant(company.simei())) {
            return RegimeTributario.MEI;
        }

        if (isOptant(company.simples())) {
            return RegimeTributario.SIMPLES_NACIONAL;
        }

        return null;
    }

    private boolean isOptant(CnpjaOfficeResponse.SimplesSimei bloco) {
        return bloco != null && Boolean.TRUE.equals(bloco.optant());
    }

    private AddressRequest toAddress(CnpjaOfficeResponse.Address address) {
        if (address == null) {
            return null;
        }

        return new AddressRequest(
                address.zip(),
                address.street(),
                address.number(),
                address.details(),
                address.district(),
                address.city(),
                address.state());
    }

    /**
     * Nenhum contato sai marcado como principal: a CNPJá não diz qual é, e a
     * regra de negócio admite no máximo um por filial. Marcar dois quebraria o
     * cadastro; quem escolhe é o usuário, no formulário.
     */
    private List<ContactRequest> toContatos(CnpjaOfficeResponse office) {
        List<ContactRequest> contatos = new ArrayList<>();

        if (office.phones() != null) {
            office.phones().forEach(phone -> contatos.add(new ContactRequest(
                    tipoDoTelefone(phone.type()),
                    concat(phone.area(), phone.number()),
                    null,
                    null)));
        }

        if (office.emails() != null) {
            office.emails().forEach(email -> contatos.add(new ContactRequest(
                    ContactType.EMAIL,
                    email.address(),
                    descricaoDoEmail(email.ownership()),
                    null)));
        }

        return contatos;
    }

    private ContactType tipoDoTelefone(String type) {
        // O contrato só prevê LANDLINE e MOBILE; um valor novo cai em TELEFONE
        // porque o tipo do contato é obrigatório no cadastro.
        return "MOBILE".equals(type) ? ContactType.CELULAR : ContactType.TELEFONE;
    }

    private String descricaoDoEmail(String ownership) {
        if (ownership == null) {
            return null;
        }

        return switch (ownership) {
            case "ACCOUNTING" -> "Contabilidade";
            case "CORPORATE" -> "Corporativo";
            case "PERSONAL" -> "Pessoal";
            default -> null;
        };
    }

    private String concat(String area, String number) {
        return (area == null ? "" : area) + (number == null ? "" : number);
    }
}
