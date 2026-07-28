package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.AddressRequest;
import br.com.luizbrand.worklog.client.dto.BranchRequest;
import br.com.luizbrand.worklog.client.dto.CnpjLookupResponse;
import br.com.luizbrand.worklog.client.dto.ContactRequest;
import br.com.luizbrand.worklog.client.enums.ClientType;
import br.com.luizbrand.worklog.client.enums.ContactType;
import br.com.luizbrand.worklog.client.enums.RegimeTributario;
import br.com.luizbrand.worklog.client.integration.CnpjaClient;
import br.com.luizbrand.worklog.client.integration.dto.CnpjaOfficeResponse;
import br.com.luizbrand.worklog.exception.BadRequest.InvalidDocumentoException;
import br.com.luizbrand.worklog.exception.NotFound.CnpjNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CnpjLookupServiceTest {

    private static final String CNPJ_AMBEV_MANAUS = "07526557011659";

    @Mock
    private CnpjaClient cnpjaClient;

    @InjectMocks
    private CnpjLookupService cnpjLookupService;

    private CnpjaOfficeResponse office(Boolean head) {
        return office(head, company(false, false), ativa(), inscricoesDaAmbev());
    }

    private CnpjaOfficeResponse office(Boolean head,
                                       CnpjaOfficeResponse.Company company,
                                       CnpjaOfficeResponse.Status status,
                                       List<CnpjaOfficeResponse.Registration> registrations) {
        return new CnpjaOfficeResponse(
                CNPJ_AMBEV_MANAUS,
                "AMBEV FILIAL MANAUS",
                head,
                company,
                new CnpjaOfficeResponse.Address("Avenida Buriti", "4000", "Galpao 2",
                        "Distrito Industrial", "Manaus", "AM", "69075000"),
                List.of(new CnpjaOfficeResponse.Phone("LANDLINE", "92", "21238000"),
                        new CnpjaOfficeResponse.Phone("MOBILE", "11", "999998888")),
                List.of(new CnpjaOfficeResponse.Email("ACCOUNTING", "contabilidade@ambev.com.br"),
                        new CnpjaOfficeResponse.Email("CORPORATE", "atendimento@ambev.com.br")),
                status,
                registrations);
    }

    private CnpjaOfficeResponse.Company company(boolean simples, boolean simei) {
        return new CnpjaOfficeResponse.Company("AMBEV S.A.",
                new CnpjaOfficeResponse.SimplesSimei(simples),
                new CnpjaOfficeResponse.SimplesSimei(simei));
    }

    private CnpjaOfficeResponse.Status ativa() {
        return new CnpjaOfficeResponse.Status(2, "Ativa");
    }

    private CnpjaOfficeResponse.Registration registration(String number, String state,
                                                          int typeId, boolean enabled) {
        return new CnpjaOfficeResponse.Registration(number, state, enabled,
                new CnpjaOfficeResponse.Registration.Type(typeId, "tipo " + typeId));
    }

    /**
     * O caso real que pega o erro: quatro inscrições, todas do mesmo
     * estabelecimento de Manaus. Só a de AM — a UF do endereço — é a dele como
     * contribuinte; as outras três são substituição tributária.
     */
    private List<CnpjaOfficeResponse.Registration> inscricoesDaAmbev() {
        return List.of(
                registration("240519525", "RR", 2, true),
                registration("159195853", "PA", 2, true),
                registration("054591406", "AM", 1, true),
                registration("0108826400193", "AC", 2, true));
    }

    @Nested
    @DisplayName("Validação do documento")
    class Validacao {

        @Test
        @DisplayName("CNPJ com checksum inválido -> 400 sem gastar consulta na CNPJá")
        void shouldRejectInvalidCnpjWithoutCallingClient() {
            assertThatThrownBy(() -> cnpjLookupService.lookup("07526557011658"))
                    .isInstanceOf(InvalidDocumentoException.class)
                    .hasMessageContaining("CNPJ inválido");

            // O limite da API pública é de 5 consultas/min por IP: gastar uma
            // requisição com documento que já sabemos inválido é desperdício.
            verifyNoInteractions(cnpjaClient);
        }

        @Test
        @DisplayName("Documento ausente -> 400 sem chamar a CNPJá")
        void shouldRejectNullDocumento() {
            assertThatThrownBy(() -> cnpjLookupService.lookup(null))
                    .isInstanceOf(InvalidDocumentoException.class);

            verifyNoInteractions(cnpjaClient);
        }

        @Test
        @DisplayName("Documento em branco -> 400 sem chamar a CNPJá")
        void shouldRejectBlankDocumento() {
            assertThatThrownBy(() -> cnpjLookupService.lookup("   "))
                    .isInstanceOf(InvalidDocumentoException.class);

            verifyNoInteractions(cnpjaClient);
        }

        @Test
        @DisplayName("CPF válido é recusado: o endpoint é de CNPJ")
        void shouldRejectCpf() {
            assertThatThrownBy(() -> cnpjLookupService.lookup("11144477735"))
                    .isInstanceOf(InvalidDocumentoException.class);

            verifyNoInteractions(cnpjaClient);
        }

        @Test
        @DisplayName("CNPJ mascarado é normalizado antes da consulta")
        void shouldNormalizeMaskedDocumento() {
            when(cnpjaClient.findOffice(CNPJ_AMBEV_MANAUS)).thenReturn(office(false));

            cnpjLookupService.lookup("07.526.557/0116-59");

            verify(cnpjaClient).findOffice(CNPJ_AMBEV_MANAUS);
        }
    }

    @Nested
    @DisplayName("Mapeamento para o formato do formulário")
    class Mapeamento {

        @Test
        @DisplayName("Campos da empresa e da filial")
        void shouldMapCompanyAndBranchFields() {
            when(cnpjaClient.findOffice(CNPJ_AMBEV_MANAUS)).thenReturn(office(false));

            CnpjLookupResponse response = cnpjLookupService.lookup(CNPJ_AMBEV_MANAUS);

            assertThat(response.tipo()).isEqualTo(ClientType.PJ);
            assertThat(response.name()).isEqualTo("AMBEV S.A.");
            assertThat(response.branches()).hasSize(1);

            BranchRequest branch = response.branches().get(0);
            assertThat(branch.documento()).isEqualTo(CNPJ_AMBEV_MANAUS);
            assertThat(branch.apelido()).isEqualTo("AMBEV FILIAL MANAUS");
            assertThat(branch.isMatriz()).isFalse();
            assertThat(branch.inscricaoEstadual()).isEqualTo("054591406");
            // Inscrição municipal não existe em nenhum ponto do contrato da CNPJá.
            assertThat(branch.inscricaoMunicipal()).isNull();
        }

        @Test
        @DisplayName("nomeFantasia só é preenchido quando o estabelecimento é a matriz")
        void shouldFillNomeFantasiaOnlyForHeadOffice() {
            when(cnpjaClient.findOffice(anyString())).thenReturn(office(false));
            assertThat(cnpjLookupService.lookup(CNPJ_AMBEV_MANAUS).nomeFantasia()).isNull();

            when(cnpjaClient.findOffice(anyString())).thenReturn(office(true));
            CnpjLookupResponse matriz = cnpjLookupService.lookup(CNPJ_AMBEV_MANAUS);

            assertThat(matriz.nomeFantasia()).isEqualTo("AMBEV FILIAL MANAUS");
            assertThat(matriz.branches().get(0).isMatriz()).isTrue();
            // O apelido do estabelecimento é preenchido nos dois casos.
            assertThat(matriz.branches().get(0).apelido()).isEqualTo("AMBEV FILIAL MANAUS");
        }

        @Test
        @DisplayName("Endereço completo")
        void shouldMapAddress() {
            when(cnpjaClient.findOffice(CNPJ_AMBEV_MANAUS)).thenReturn(office(false));

            AddressRequest address = cnpjLookupService.lookup(CNPJ_AMBEV_MANAUS)
                    .branches().get(0).address();

            assertThat(address).isNotNull();
            assertThat(address.logradouro()).isEqualTo("Avenida Buriti");
            assertThat(address.numero()).isEqualTo("4000");
            assertThat(address.complemento()).isEqualTo("Galpao 2");
            assertThat(address.bairro()).isEqualTo("Distrito Industrial");
            assertThat(address.cidade()).isEqualTo("Manaus");
            assertThat(address.uf()).isEqualTo("AM");
            assertThat(address.cep()).isEqualTo("69075000");
        }

        @Test
        @DisplayName("Telefones e e-mails viram contatos, nenhum marcado como principal")
        void shouldMapPhonesAndEmailsToContacts() {
            when(cnpjaClient.findOffice(CNPJ_AMBEV_MANAUS)).thenReturn(office(false));

            List<ContactRequest> contatos = cnpjLookupService.lookup(CNPJ_AMBEV_MANAUS)
                    .branches().get(0).contatos();

            assertThat(contatos).hasSize(4);
            assertThat(contatos.get(0).tipo()).isEqualTo(ContactType.TELEFONE);
            assertThat(contatos.get(0).valor()).isEqualTo("9221238000");
            assertThat(contatos.get(1).tipo()).isEqualTo(ContactType.CELULAR);
            assertThat(contatos.get(1).valor()).isEqualTo("11999998888");
            assertThat(contatos.get(2).tipo()).isEqualTo(ContactType.EMAIL);
            assertThat(contatos.get(2).valor()).isEqualTo("contabilidade@ambev.com.br");
            assertThat(contatos.get(2).descricao()).isEqualTo("Contabilidade");
            assertThat(contatos.get(3).tipo()).isEqualTo(ContactType.EMAIL);
            assertThat(contatos.get(3).descricao()).isEqualTo("Corporativo");

            // A CNPJá não diz qual contato é o principal e a regra 7.5 admite no
            // máximo um por filial: quem escolhe é o usuário, no formulário.
            assertThat(contatos).allSatisfy(contato -> assertThat(contato.principal()).isNull());
        }

        @Test
        @DisplayName("Resposta sem endereço, telefone ou e-mail não quebra o mapeamento")
        void shouldTolerateMissingOptionalBlocks() {
            CnpjaOfficeResponse magro = new CnpjaOfficeResponse(
                    CNPJ_AMBEV_MANAUS, null, null,
                    new CnpjaOfficeResponse.Company("AMBEV S.A.", null, null),
                    null, null, null, null, null);
            when(cnpjaClient.findOffice(CNPJ_AMBEV_MANAUS)).thenReturn(magro);

            CnpjLookupResponse response = cnpjLookupService.lookup(CNPJ_AMBEV_MANAUS);

            BranchRequest branch = response.branches().get(0);
            assertThat(response.nomeFantasia()).isNull();
            assertThat(branch.apelido()).isNull();
            assertThat(branch.isMatriz()).isFalse();
            assertThat(branch.address()).isNull();
            assertThat(branch.contatos()).isEmpty();
            assertThat(branch.inscricaoEstadual()).isNull();
            assertThat(response.regimeTributario()).isNull();
            assertThat(response.situacaoCadastral()).isNull();
            assertThat(response.situacaoAtiva()).isFalse();
        }
    }

    @Nested
    @DisplayName("Inscrição estadual: a da UF do endereço")
    class InscricaoEstadual {

        @Test
        @DisplayName("Entre as quatro inscrições da Ambev, escolhe a IE Normal da UF do endereço")
        void shouldPickRegistrationOfTheAddressState() {
            when(cnpjaClient.findOffice(CNPJ_AMBEV_MANAUS)).thenReturn(office(false));

            // O endereço é AM. As outras três são substituição tributária em
            // estados para onde o estabelecimento vende — pegar a primeira da
            // lista traria a de RR.
            assertThat(inscricaoEstadual()).isEqualTo("054591406");
        }

        @Test
        @DisplayName("Prefere IE Normal quando a UF tem mais de uma inscrição")
        void shouldPreferNormalRegistration() {
            when(cnpjaClient.findOffice(CNPJ_AMBEV_MANAUS)).thenReturn(office(false,
                    company(false, false), ativa(),
                    List.of(registration("999888777", "AM", 2, true),
                            registration("054591406", "AM", 1, true))));

            assertThat(inscricaoEstadual()).isEqualTo("054591406");
        }

        @Test
        @DisplayName("Entre duas IE Normal da UF, prefere a habilitada")
        void shouldPreferEnabledRegistration() {
            when(cnpjaClient.findOffice(CNPJ_AMBEV_MANAUS)).thenReturn(office(false,
                    company(false, false), ativa(),
                    List.of(registration("111222333", "AM", 1, false),
                            registration("054591406", "AM", 1, true))));

            assertThat(inscricaoEstadual()).isEqualTo("054591406");
        }

        @Test
        @DisplayName("Sem IE Normal na UF, usa a que houver")
        void shouldFallBackToAnyRegistrationOfTheState() {
            when(cnpjaClient.findOffice(CNPJ_AMBEV_MANAUS)).thenReturn(office(false,
                    company(false, false), ativa(),
                    List.of(registration("777666555", "AM", 2, true))));

            assertThat(inscricaoEstadual()).isEqualTo("777666555");
        }

        @Test
        @DisplayName("Nenhuma inscrição na UF do endereço -> em branco")
        void shouldLeaveBlankWhenNoRegistrationMatchesTheState() {
            when(cnpjaClient.findOffice(CNPJ_AMBEV_MANAUS)).thenReturn(office(false,
                    company(false, false), ativa(),
                    List.of(registration("240519525", "RR", 1, true))));

            assertThat(inscricaoEstadual()).isNull();
        }

        @Test
        @DisplayName("Sem lista de inscrições -> em branco")
        void shouldLeaveBlankWhenRegistrationsAreAbsent() {
            when(cnpjaClient.findOffice(CNPJ_AMBEV_MANAUS))
                    .thenReturn(office(false, company(false, false), ativa(), null));

            assertThat(inscricaoEstadual()).isNull();
        }

        @Test
        @DisplayName("Sem endereço não há UF para comparar -> em branco")
        void shouldLeaveBlankWhenAddressIsAbsent() {
            CnpjaOfficeResponse semEndereco = new CnpjaOfficeResponse(
                    CNPJ_AMBEV_MANAUS, null, false, company(false, false),
                    null, null, null, ativa(), inscricoesDaAmbev());
            when(cnpjaClient.findOffice(CNPJ_AMBEV_MANAUS)).thenReturn(semEndereco);

            assertThat(inscricaoEstadual()).isNull();
        }

        private String inscricaoEstadual() {
            return cnpjLookupService.lookup(CNPJ_AMBEV_MANAUS)
                    .branches().get(0).inscricaoEstadual();
        }
    }

    @Nested
    @DisplayName("Regime tributário")
    class Regime {

        @Test
        @DisplayName("MEI vence Simples: todo MEI é optante do Simples")
        void shouldPreferMeiOverSimples() {
            // Este é o caso que a ordem invertida erra em silêncio — a CNPJá
            // devolve simples.optant = true para todo MEI, então checar Simples
            // primeiro cadastraria todo MEI como SIMPLES_NACIONAL.
            when(cnpjaClient.findOffice(CNPJ_AMBEV_MANAUS))
                    .thenReturn(office(false, company(true, true), ativa(), null));

            assertThat(regime()).isEqualTo(RegimeTributario.MEI);
        }

        @Test
        @DisplayName("Optante do Simples e não MEI -> SIMPLES_NACIONAL")
        void shouldMapSimplesOptant() {
            when(cnpjaClient.findOffice(CNPJ_AMBEV_MANAUS))
                    .thenReturn(office(false, company(true, false), ativa(), null));

            assertThat(regime()).isEqualTo(RegimeTributario.SIMPLES_NACIONAL);
        }

        @Test
        @DisplayName("Nem Simples nem MEI -> em branco, o usuário escolhe")
        void shouldLeaveBlankWhenNeither() {
            // A API não informa Lucro Presumido nem Lucro Real: adivinhar aqui
            // seria inventar dado fiscal.
            when(cnpjaClient.findOffice(CNPJ_AMBEV_MANAUS))
                    .thenReturn(office(false, company(false, false), ativa(), null));

            assertThat(regime()).isNull();
        }

        @Test
        @DisplayName("Blocos simples/simei ausentes -> em branco")
        void shouldLeaveBlankWhenBlocksAreAbsent() {
            when(cnpjaClient.findOffice(CNPJ_AMBEV_MANAUS)).thenReturn(office(false,
                    new CnpjaOfficeResponse.Company("AMBEV S.A.", null, null), ativa(), null));

            assertThat(regime()).isNull();
        }

        private RegimeTributario regime() {
            return cnpjLookupService.lookup(CNPJ_AMBEV_MANAUS).regimeTributario();
        }
    }

    @Nested
    @DisplayName("Situação cadastral")
    class Situacao {

        @Test
        @DisplayName("Ativa -> situacaoAtiva true")
        void shouldFlagActiveStatus() {
            when(cnpjaClient.findOffice(CNPJ_AMBEV_MANAUS)).thenReturn(office(false));

            CnpjLookupResponse response = cnpjLookupService.lookup(CNPJ_AMBEV_MANAUS);

            assertThat(response.situacaoCadastral()).isEqualTo("Ativa");
            assertThat(response.situacaoAtiva()).isTrue();
        }

        @Test
        @DisplayName("Baixada -> preenche os dados assim mesmo e avisa (D8)")
        void shouldStillFillDataForInactiveStatus() {
            when(cnpjaClient.findOffice(CNPJ_AMBEV_MANAUS)).thenReturn(office(false,
                    company(false, false),
                    new CnpjaOfficeResponse.Status(8, "Baixada"),
                    inscricoesDaAmbev()));

            CnpjLookupResponse response = cnpjLookupService.lookup(CNPJ_AMBEV_MANAUS);

            assertThat(response.situacaoCadastral()).isEqualTo("Baixada");
            assertThat(response.situacaoAtiva()).isFalse();
            // Recusar a consulta impediria o cadastro de cliente que existe e
            // ainda é atendido: a decisão é do atendente, não do backend.
            assertThat(response.name()).isEqualTo("AMBEV S.A.");
            assertThat(response.branches().get(0).documento()).isEqualTo(CNPJ_AMBEV_MANAUS);
            assertThat(response.branches().get(0).inscricaoEstadual()).isEqualTo("054591406");
        }

        @Test
        @DisplayName("Sem bloco de situação -> não afirma que está ativa")
        void shouldNotClaimActiveWhenStatusIsAbsent() {
            when(cnpjaClient.findOffice(CNPJ_AMBEV_MANAUS))
                    .thenReturn(office(false, company(false, false), null, null));

            CnpjLookupResponse response = cnpjLookupService.lookup(CNPJ_AMBEV_MANAUS);

            assertThat(response.situacaoCadastral()).isNull();
            assertThat(response.situacaoAtiva()).isFalse();
        }
    }

    @Test
    @DisplayName("Erro traduzido pelo client sobe intacto para o controller")
    void shouldPropagateClientExceptions() {
        when(cnpjaClient.findOffice(CNPJ_AMBEV_MANAUS))
                .thenThrow(new CnpjNotFoundException("CNPJ não encontrado"));

        assertThatThrownBy(() -> cnpjLookupService.lookup(CNPJ_AMBEV_MANAUS))
                .isInstanceOf(CnpjNotFoundException.class);
    }
}
