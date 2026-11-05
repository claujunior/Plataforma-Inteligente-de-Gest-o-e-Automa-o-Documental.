package ufrn.bcc.service_alpha.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ufrn.bcc.service_alpha.Model.Funcionario;

public interface FuncionarioRepository extends JpaRepository<Funcionario, String> {
}
