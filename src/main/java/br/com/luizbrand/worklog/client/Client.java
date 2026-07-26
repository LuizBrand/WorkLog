package br.com.luizbrand.worklog.client;

import br.com.luizbrand.worklog.client.enums.ClientType;
import br.com.luizbrand.worklog.client.enums.RegimeTributario;
import br.com.luizbrand.worklog.shared.BaseEntity;
import br.com.luizbrand.worklog.system.Systems;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
@Entity
@Table(name = "clients")
public class Client extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "client_system",
            joinColumns = @JoinColumn(name = "client_id"),
            inverseJoinColumns = @JoinColumn(name = "system_id")
    )
    private List<Systems> systems;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 2)
    private ClientType tipo;

    @Column(name = "nome_fantasia", length = 100)
    private String nomeFantasia;

    @Enumerated(EnumType.STRING)
    @Column(name = "regime_tributario", length = 30)
    private RegimeTributario regimeTributario;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ClientBranch> branches = new ArrayList<>();

}
