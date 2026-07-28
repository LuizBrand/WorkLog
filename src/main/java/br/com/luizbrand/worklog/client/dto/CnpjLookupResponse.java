package br.com.luizbrand.worklog.client.dto;

import br.com.luizbrand.worklog.client.enums.ClientType;
import br.com.luizbrand.worklog.client.enums.RegimeTributario;

import java.util.List;

/**
 * Resultado da consulta de CNPJ, já no formato do formulário de cadastro.
 *
 * <p>Reusa {@link BranchRequest} de propósito: o front recebe exatamente a
 * estrutura que vai devolver no {@code POST /clients}, sem tradução no meio.
 * Nada aqui é persistido — o usuário revisa e corrige antes de salvar.
 *
 * <p>{@code situacaoCadastral} e {@code situacaoAtiva} fogem desse formato de
 * propósito: não são campos do cadastro, e sim o aviso da tela. CNPJ suspenso,
 * inapto ou baixado vem preenchido do mesmo jeito — quem decide se cadastra é
 * o atendente.
 */
public record CnpjLookupResponse(
        ClientType tipo,
        String name,
        String nomeFantasia,
        RegimeTributario regimeTributario,
        String situacaoCadastral,
        boolean situacaoAtiva,
        List<BranchRequest> branches) {
}
