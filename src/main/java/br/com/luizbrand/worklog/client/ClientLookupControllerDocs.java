package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.dto.CnpjLookupResponse;
import br.com.luizbrand.worklog.exceptionhandler.ApiExceptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Clientes", description = "Gerenciamento de clientes e seus sistemas associados")
public interface ClientLookupControllerDocs {

    @Operation(summary = "Consultar CNPJ na Receita Federal",
            description = """
                    Consulta o CNPJ na CNPJá e devolve os dados já no formato do formulário de
                    cadastro, prontos para o `POST /clients`. **Nada é persistido**: o usuário
                    revisa e corrige antes de salvar.

                    O documento é validado localmente antes da consulta — CNPJ com checksum
                    inválido devolve 400 sem gastar requisição no provedor, que limita a
                    5 consultas por minuto por endereço IP.

                    A inscrição estadual devolvida é a da **UF do endereço** do
                    estabelecimento, preferindo IE Normal e habilitada — um mesmo CNPJ pode
                    ter várias inscrições, as demais sendo substituição tributária em
                    estados para onde ele vende. Sem inscrição na UF, volta em branco.

                    O regime tributário só é preenchido quando o provedor afirma: MEI ou
                    Simples Nacional. Lucro Presumido e Lucro Real não existem no contrato e
                    ficam a cargo do usuário.

                    `situacaoCadastral` e `situacaoAtiva` são o aviso da tela, não campos do
                    cadastro: CNPJ suspenso, inapto ou baixado vem **preenchido do mesmo
                    jeito**, com `situacaoAtiva: false`, e quem decide se cadastra é o
                    atendente.

                    A inscrição municipal não existe no contrato do provedor e volta sempre
                    em branco. Nenhum contato vem marcado como principal: quem escolhe é o
                    usuário.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta realizada com sucesso",
                    content = @Content(schema = @Schema(implementation = CnpjLookupResponse.class))),
            @ApiResponse(responseCode = "400", description = "CNPJ ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "CNPJ não registrado na Receita Federal",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "429", description = "Limite de consultas do provedor atingido; tente novamente em instantes",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class))),
            @ApiResponse(responseCode = "503", description = "Provedor de consulta indisponível; preencha o cadastro manualmente",
                    content = @Content(schema = @Schema(implementation = ApiExceptionResponse.class)))
    })
    ResponseEntity<CnpjLookupResponse> lookupByCnpj(
            @Parameter(description = "CNPJ do estabelecimento, com ou sem máscara", required = true,
                    example = "07526557011659") String documento);
}
