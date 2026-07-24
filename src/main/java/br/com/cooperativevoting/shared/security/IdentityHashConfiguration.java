package br.com.cooperativevoting.shared.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    IdentityHashProperties.class,
    ApiKeyProperties.class
})
public class IdentityHashConfiguration {
}
