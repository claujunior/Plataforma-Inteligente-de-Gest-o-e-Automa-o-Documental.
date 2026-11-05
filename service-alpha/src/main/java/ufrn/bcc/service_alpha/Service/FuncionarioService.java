package ufrn.bcc.service_alpha.Service;


import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ufrn.bcc.service_alpha.Dto.AtualizarFuncionarioRequest;
import ufrn.bcc.service_alpha.Dto.CadastrarFuncionarioRequest;
import ufrn.bcc.service_alpha.Model.Funcionario;
import ufrn.bcc.service_alpha.Repository.FuncionarioRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;

    public FuncionarioService(FuncionarioRepository funcionarioRepository) {
        this.funcionarioRepository = funcionarioRepository;
    }

    public List<Funcionario> listar() {
        return funcionarioRepository.findAll(Sort.by("nome", "cpf"));
    }

    public Funcionario buscar(String cpf) {
        return funcionarioRepository.findById(cpf)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Funcionário não encontrado."));
    }

    @Transactional
    public Funcionario cadastrar(CadastrarFuncionarioRequest request) {
        Funcionario funcionario = new Funcionario(
                request.cpf(), request.nome(), request.numeroTelefone(),
                request.email(), request.salario());
        funcionarioRepository.save(funcionario);
        return funcionario;
    }

    @Transactional
    public Funcionario atualizar(String cpf, AtualizarFuncionarioRequest request) {
        Funcionario funcionario = buscar(cpf);
        funcionario.setNome(request.nome());
        funcionario.setSalario(request.salario());
        funcionario.setNumeroTelefone(request.numeroTelefone());
        funcionario.setEmail(request.email());
        return funcionario;
    }

    @Transactional
    public void excluir(String cpf) {
        funcionarioRepository.delete(buscar(cpf));
    }
}
