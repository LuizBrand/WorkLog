package br.com.luizbrand.worklog.client;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Address {

    @Column(length = 8)
    private String cep;

    @Column( length = 150)
    private String logradouro;

   @Column(length = 20)
   private String numero;

   @Column(length = 100)
   private String complemento;

   @Column(length = 100)
   private String bairro;

   @Column(length = 100)
   private String cidade;

   @Column(length = 2)
   private String uf;

}
