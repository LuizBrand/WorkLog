package br.com.luizbrand.worklog.client.dto;

import br.com.luizbrand.worklog.client.enums.ClientType;

import java.util.List;

/**
 * Resultado da consulta de CNPJ, já no formato do formulário de cadastro.
 *
 * <p>Reusa {@link BranchRequest} de propósito: o front recebe exatamente a
 * estrutura que vai devolver no {@code POST /clients}, sem tradução no meio.
 * Nada aqui é persistido — o usuário revisa e corrige antes de salvar.
 */
public record CnpjLookupResponse(
        ClientType tipo,
        String name,
        String nomeFantasia,
        List<BranchRequest> branches) {
}
