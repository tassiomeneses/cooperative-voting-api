package br.com.cooperativevoting.shared.documentation;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI cooperativeVotingOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Cooperative Voting API")
                .description("API REST para cadastro de pautas, abertura de sessões, registro de votos e apuração de resultados.")
                .version("v1")
                .contact(new Contact()
                    .name("Technical Challenge"))
                .license(new License()
                    .name("Internal use")))
            .servers(List.of(new Server()
                .url("/")
                .description("Servidor atual")));
    }
}
