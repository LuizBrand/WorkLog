package br.com.luizbrand.worklog.system;

import br.com.luizbrand.worklog.client.Client;
import br.com.luizbrand.worklog.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@Entity
@Table(name = "systems")
public class Systems extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToMany(mappedBy = "systems")
    private List<Client> clients;

}
