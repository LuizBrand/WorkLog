package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@Entity
@Table(name = "client_branches")
public class ClientBranch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(length = 20, unique = true)
    private String documento;

    @Column(name = "is_matriz", nullable = false)
    @Builder.Default
    private Boolean isMatriz = false;

    @Column(length = 100)
    private String apelido;

    @Column(name = "inscricao_estadual", length = 20)
    private String inscricaoEstadual;

    @Column(name = "inscricao_municipal", length = 20)
    private String inscricaoMunicipal;

    @Embedded
    private Address address;

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ClientContact> contatos = new ArrayList<>();

}
