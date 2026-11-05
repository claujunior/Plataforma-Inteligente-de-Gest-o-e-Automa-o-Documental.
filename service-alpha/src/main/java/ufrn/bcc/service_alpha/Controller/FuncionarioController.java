package ufrn.bcc.service_alpha.Controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ufrn.bcc.service_alpha.Dto.AtualizarFuncionarioRequest;
import ufrn.bcc.service_alpha.Dto.CadastrarFuncionarioRequest;
import ufrn.bcc.service_alpha.Model.Funcionario;
import ufrn.bcc.service_alpha.Service.FuncionarioService;

import java.util.List;

@RestController
@RequestMapping("/funcionarios")
public class FuncionarioController {

    private final FuncionarioService funcionarioService;

    public FuncionarioController(FuncionarioService funcionarioService) {
        this.funcionarioService = funcionarioService;
    }

    @GetMapping
    public List<Funcionario> listar() {
        return funcionarioService.listar();
    }

    @GetMapping("/{cpf}")
    public Funcionario buscar(@PathVariable String cpf) {
        return funcionarioService.buscar(cpf);
    }

    @PostMapping
    public ResponseEntity<String> cadastrar(@Valid @RequestBody CadastrarFuncionarioRequest request) {
        funcionarioService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("Funcionário cadastrado com sucesso.");
    }

    @PutMapping("/{cpf}")
    public Funcionario atualizar(
            @PathVariable String cpf,
            @Valid @RequestBody AtualizarFuncionarioRequest request) {
        return funcionarioService.atualizar(cpf, request);
    }

    @DeleteMapping("/{cpf}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable String cpf) {
        funcionarioService.excluir(cpf);
    }
}
