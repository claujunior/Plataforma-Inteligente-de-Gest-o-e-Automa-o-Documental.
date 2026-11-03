package ufrn.bcc.ApiGateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class TimeLimit {
    @Bean
    public KeyResolver ipKeyResolver(){
        return exchange -> Mono.just("todos");
    }
}
