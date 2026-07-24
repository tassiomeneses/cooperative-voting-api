package br.com.cooperativevoting.shared.security;

import br.com.cooperativevoting.voting.domain.model.vo.Cpf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component("cpfHashingService")
public class CpfHashingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CpfHashingService.class);
    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] pepper;

    public CpfHashingService(IdentityHashProperties properties) {
        this.pepper = properties.pepper().getBytes(StandardCharsets.UTF_8);
        if (IdentityHashProperties.DEFAULT_PEPPER.equals(properties.pepper())) {
            if (properties.requireCustomPepper()) {
                throw new IllegalStateException("IDENTITY_HASH_PEPPER must be configured outside local development.");
            }
            LOGGER.warn("Using default identity hash pepper. Configure IDENTITY_HASH_PEPPER outside local development.");
        }
    }

    public String hash(Cpf cpf) {
        return hash(cpf.value());
    }

    public String hash(String rawValue) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(pepper, ALGORITHM));
            byte[] digest = mac.doFinal(rawValue.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash CPF identity", exception);
        }
    }
}
