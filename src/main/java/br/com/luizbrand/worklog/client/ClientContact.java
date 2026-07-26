package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.enums.ContactType;
import br.com.luizbrand.worklog.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
@Entity
@Table(name = "client_contacts")
public class ClientContact extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private ClientBranch branch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContactType tipo;

    @Column(nullable = false, length = 150)
    private String valor;

    @Column(length = 100)
    private String descricao;

    @Builder.Default
    private Boolean principal = false;

}