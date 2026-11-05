package ufrn.bcc.service_alpha.Dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import java.math.BigDecimal;

public record CadastrarFuncionarioRequest(

        @NotBlank
        @Pattern(regexp = "[0-9]{11}", message = "Informe 11 dígitos.")
        @CPF
        String cpf,

        @NotBlank
        @Size(max = 150)
        String nome,

        @NotNull
        @PositiveOrZero
        @Digits(integer = 10, fraction = 2)
        BigDecimal salario,

        @Pattern(
                regexp = "\\+?[0-9]{10,15}",
                message = "Informe de 10 a 15 dígitos, com + opcional."
        )
        String numeroTelefone,

        @Email
        @Size(min = 1, max = 254)
        String email

) {}
