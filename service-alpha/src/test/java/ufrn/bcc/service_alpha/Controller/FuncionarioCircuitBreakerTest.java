package ufrn.bcc.service_alpha.Controller;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import ufrn.bcc.service_alpha.Dto.AtualizarFuncionarioRequest;
import ufrn.bcc.service_alpha.Dto.CadastrarFuncionarioRequest;
import ufrn.bcc.service_alpha.Exception.GlobalExceptionHandler;
import ufrn.bcc.service_alpha.Model.Funcionario;
import ufrn.bcc.service_alpha.Service.FuncionarioService;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FuncionarioCircuitBreakerTest {

    private static final String CPF = "52998224725";
    private FuncionarioService service;
    private CircuitBreakerRegistry registry;
    private FuncionarioController controller;

    @BeforeEach
    void preparar() {
        service = mock(FuncionarioService.class);
        var config = CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .ignoreExceptions(ResponseStatusException.class, DataIntegrityViolationException.class)
                .build();
        registry = CircuitBreakerRegistry.of(config);
        controller = new FuncionarioController(service, registry);
    }

    @Test
    void falhasDeLeituraAbremSomenteOCircuitoDeLeitura() {
        when(service.listar()).thenThrow(new DataAccessResourceFailureException("Banco indisponivel."));

        for (int i = 0; i < 2; i++) {
            assertThrows(DataAccessResourceFailureException.class, controller::listar);
        }
        assertThrows(CallNotPermittedException.class, controller::listar);
        verify(service, times(2)).listar();

        var request = cadastro();
        var funcionario = funcionario();
        when(service.cadastrar(request)).thenReturn(funcionario);

        assertThat(controller.cadastrar(request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registry.circuitBreaker("funcionarios-leitura").getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(registry.circuitBreaker("funcionarios-escrita").getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void falhasDeEscritaAbremSomenteOCircuitoDeEscrita() {
        var request = cadastro();
        when(service.cadastrar(request)).thenThrow(new DataAccessResourceFailureException("Banco indisponivel."));

        for (int i = 0; i < 2; i++) {
            assertThrows(DataAccessResourceFailureException.class, () -> controller.cadastrar(request));
        }
        assertThrows(CallNotPermittedException.class, () -> controller.cadastrar(request));
        verify(service, times(2)).cadastrar(request);
        when(service.listar()).thenReturn(List.of(funcionario()));

        assertThat(controller.listar()).hasSize(1);
        assertThat(registry.circuitBreaker("funcionarios-escrita").getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(registry.circuitBreaker("funcionarios-leitura").getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void funcionarioNaoEncontradoNaoAbreCircuitoDeLeitura() {
        var erro = new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionario nao encontrado.");
        when(service.buscar(CPF)).thenThrow(erro);

        for (int i = 0; i < 3; i++) {
            assertThat(assertThrows(ResponseStatusException.class, () -> controller.buscar(CPF))).isSameAs(erro);
        }

        var circuito = registry.circuitBreaker("funcionarios-leitura");
        assertThat(circuito.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuito.getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    void duplicidadeNaoAbreCircuitoDeEscrita() {
        var request = cadastro();
        var erro = new DataIntegrityViolationException("CPF ja cadastrado.");
        when(service.cadastrar(request)).thenThrow(erro);

        for (int i = 0; i < 3; i++) {
            assertThat(assertThrows(DataIntegrityViolationException.class,
                    () -> controller.cadastrar(request))).isSameAs(erro);
        }

        var circuito = registry.circuitBreaker("funcionarios-escrita");
        assertThat(circuito.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuito.getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    void circuitoDeEscritaAbertoBloqueiaCadastroAtualizacaoEExclusao() {
        registry.circuitBreaker("funcionarios-escrita").transitionToOpenState();
        var atualizacao = new AtualizarFuncionarioRequest("Ana Souza", new BigDecimal("4200.50"), null, null);

        assertThrows(CallNotPermittedException.class, () -> controller.cadastrar(cadastro()));
        assertThrows(CallNotPermittedException.class, () -> controller.atualizar(CPF, atualizacao));
        assertThrows(CallNotPermittedException.class, () -> controller.excluir(CPF));

        verifyNoInteractions(service);
    }

    @Test
    void circuitoDeLeituraAbertoBloqueiaAsDuasConsultasEResponde503() throws Exception {
        registry.circuitBreaker("funcionarios-leitura").transitionToOpenState();

        assertThrows(CallNotPermittedException.class, () -> controller.buscar(CPF));
        var mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(get("/funcionarios"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Servico de funcionarios temporariamente indisponivel. Tente novamente em instantes."));

        verifyNoInteractions(service);
    }

    private CadastrarFuncionarioRequest cadastro() {
        return new CadastrarFuncionarioRequest(CPF, "Ana Silva", new BigDecimal("3500.00"), null, null);
    }

    private Funcionario funcionario() {
        return new Funcionario(CPF, "Ana Silva", null, null, new BigDecimal("3500.00"));
    }
}
