package ufrn.bcc.service_alpha.Service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ufrn.bcc.service_alpha.Dto.AtualizarFuncionarioRequest;
import ufrn.bcc.service_alpha.Dto.CadastrarFuncionarioRequest;
import ufrn.bcc.service_alpha.Model.Funcionario;
import ufrn.bcc.service_alpha.Repository.FuncionarioRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FuncionarioServiceTest {

    private static final String CPF = "52998224725";

    private FuncionarioRepository repository;
    private EntityManager entityManager;
    private FuncionarioService service;

    @BeforeEach
    void preparar() {
        repository = mock(FuncionarioRepository.class);
        entityManager = mock(EntityManager.class);
        service = new FuncionarioService(repository, entityManager);
    }

    @Test
    void listaFuncionariosPorNomeECpf() {
        var funcionario = funcionarioExistente();
        when(repository.findAll(Sort.by("nome", "cpf"))).thenReturn(List.of(funcionario));

        assertThat(service.listar()).containsExactly(funcionario);
    }

    @Test
    void buscaFuncionarioExistentePeloCpf() {
        var funcionario = funcionarioExistente();
        when(repository.findById(CPF)).thenReturn(Optional.of(funcionario));

        assertThat(service.buscar(CPF)).isSameAs(funcionario);
    }

    @Test
    void responde404QuandoCpfNaoExiste() {
        when(repository.findById(CPF)).thenReturn(Optional.empty());

        var erro = assertThrows(ResponseStatusException.class, () -> service.buscar(CPF));

        assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cadastraFuncionarioComOsDadosRecebidos() {
        var request = new CadastrarFuncionarioRequest(
                CPF, "Ana Silva", new BigDecimal("3500.00"), "84999990000", "ana@example.com");

        var resultado = service.cadastrar(request);

        verify(entityManager).persist(resultado);
        verifyNoInteractions(repository);
        assertThat(resultado.getCpf()).isEqualTo(request.cpf());
        assertThat(resultado.getNome()).isEqualTo(request.nome());
        assertThat(resultado.getSalario()).isEqualByComparingTo(request.salario());
        assertThat(resultado.getNumeroTelefone()).isEqualTo(request.numeroTelefone());
        assertThat(resultado.getEmail()).isEqualTo(request.email());
    }

    @Test
    void atualizaCamposEditaveisEMantemCpf() {
        var funcionario = funcionarioExistente();
        when(repository.findById(CPF)).thenReturn(Optional.of(funcionario));
        var request = new AtualizarFuncionarioRequest(
                "Ana Souza", new BigDecimal("4200.50"), null, null);

        var resultado = service.atualizar(CPF, request);

        assertThat(resultado).isSameAs(funcionario);
        assertThat(resultado.getCpf()).isEqualTo(CPF);
        assertThat(resultado.getNome()).isEqualTo("Ana Souza");
        assertThat(resultado.getSalario()).isEqualByComparingTo("4200.50");
        assertThat(resultado.getNumeroTelefone()).isNull();
        assertThat(resultado.getEmail()).isNull();
    }

    @Test
    void excluiFuncionarioEncontradoPeloCpf() {
        var funcionario = funcionarioExistente();
        when(repository.findById(CPF)).thenReturn(Optional.of(funcionario));

        service.excluir(CPF);

        verify(repository).delete(funcionario);
    }

    private Funcionario funcionarioExistente() {
        return new Funcionario(CPF, "Ana Silva", "84999990000", "ana@example.com", new BigDecimal("3500.00"));
    }
}
