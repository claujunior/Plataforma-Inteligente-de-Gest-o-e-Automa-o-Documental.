package ufrn.bcc.service_alpha.Exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException excecao, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        excecao.getBindingResult().getFieldErrors()
                .forEach(erro -> campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, "Confira os campos informados.");
        problema.setProperty("campos", campos);
        return handleExceptionInternal(excecao, problema, headers, status, request);
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ProblemDetail tratarCircuitoAberto(CallNotPermittedException excecao) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Servico de funcionarios temporariamente indisponivel. Tente novamente em instantes.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail tratarDuplicidade(DataIntegrityViolationException excecao) {
        for (Throwable causa = excecao; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacao
                    && "23505".equals(violacao.getSQLState())) {
                String mensagem = switch (violacao.getConstraintName()) {
                    case "funcionarios_pkey" -> "CPF já cadastrado.";
                    case "uk_funcionarios_nome" -> "Nome já cadastrado.";
                    case "uk_funcionarios_numero_telefone" -> "Telefone já cadastrado.";
                    case "uk_funcionarios_email" -> "E-mail já cadastrado.";
                    case null, default -> "Já existe um registro com os dados informados.";
                };
                return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, mensagem);
            }
        }

        throw excecao;
    }
}
