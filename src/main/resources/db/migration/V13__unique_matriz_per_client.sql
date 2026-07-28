-- Garante "no máximo uma matriz por cliente" no banco.
--
-- Até a fase 4 a regra vivia só no ClientService, e isso bastava porque a filial
-- nascia junto com o cliente e era imutável. Com POST e PATCH de filial, duas
-- requisições concorrentes passam as duas pela checagem em memória e o cliente
-- termina com duas matrizes — sem nada no banco para impedir.
--
-- Índice PARCIAL (WHERE is_matriz): as filiais comuns compartilham client_id à
-- vontade; a restrição só vale para as linhas marcadas como matriz.
--
-- ATENÇÃO ANTES DO DEPLOY EM PRODUÇÃO: rodar a query abaixo. Se ela devolver
-- qualquer linha, esta migração falha e derruba o boot.
--
--   SELECT client_id, count(*)
--     FROM client_branches
--    WHERE is_matriz
--    GROUP BY client_id
--   HAVING count(*) > 1;
--
-- Verificado em dev em 2026-07-26: 7 filiais, 7 matrizes, nenhum cliente com
-- duas.

CREATE UNIQUE INDEX ux_client_branches_matriz
    ON client_branches (client_id)
    WHERE is_matriz;
