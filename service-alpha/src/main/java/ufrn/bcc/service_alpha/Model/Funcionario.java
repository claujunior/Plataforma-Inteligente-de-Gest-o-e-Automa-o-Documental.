package ufrn.bcc.service_alpha.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "funcionarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Funcionario {

    @Id
    @Setter(AccessLevel.NONE)
    @Column(nullable = false, length = 11)
    private String cpf;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "numero_telefone",nullable = true, length = 20)
    private String numeroTelefone;

    @Column(nullable = true, length = 254)
    private String email;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal salario;
}