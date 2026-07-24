package br.com.cooperativevoting.voting.adapter.out.client.userinfo;

import br.com.cooperativevoting.shared.security.CpfHashingService;
import br.com.cooperativevoting.shared.security.IdentityHashConfiguration;
import br.com.cooperativevoting.voting.application.exception.AssociateEligibilityUnavailableException;
import br.com.cooperativevoting.voting.application.port.out.AssociateEligibilityPort;
import br.com.cooperativevoting.voting.domain.model.enums.AssociateVotingStatus;
import br.com.cooperativevoting.voting.domain.model.vo.Cpf;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static br.com.cooperativevoting.voting.adapter.out.client.userinfo.UserInfoClientConfiguration.ASSOCIATE_ELIGIBILITY_CACHE;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = UserInfoAssociateEligibilityAdapterTest.TestApplication.class)
class UserInfoAssociateEligibilityAdapterTest {

    private static final String CPF = "52998224725";
    private static final String CPF_WITH_MASK = "529.982.247-25";
    private static final String USER_INFO_PATH = "/users/" + CPF;

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @Autowired
    private AssociateEligibilityPort associateEligibilityPort;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CpfHashingService cpfHashingService;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("integrations.user-info.base-url", wireMock::baseUrl);
        registry.add("integrations.user-info.retry.period", () -> "10ms");
        registry.add("integrations.user-info.retry.max-period", () -> "10ms");
        registry.add("integrations.user-info.retry.max-attempts", () -> "2");
        registry.add("integrations.user-info.cache.ttl", () -> "10m");
        registry.add("integrations.user-info.cache.maximum-size", () -> "100");
        registry.add("security.identity-hash.pepper", () -> "test-pepper");
        registry.add("spring.cloud.openfeign.circuitbreaker.enabled", () -> "true");
        registry.add("spring.cloud.openfeign.client.config.user-info.connectTimeout", () -> "1000");
        registry.add("spring.cloud.openfeign.client.config.user-info.readTimeout", () -> "5000");
        registry.add("resilience4j.circuitbreaker.instances.userInfo.minimumNumberOfCalls", () -> "100");
        registry.add("resilience4j.timelimiter.instances.userInfo.timeoutDuration", () -> "6s");
    }

    @BeforeEach
    void setUp() {
        configureFor("localhost", wireMock.getPort());
        wireMock.resetAll();
        Cache cache = cacheManager.getCache(ASSOCIATE_ELIGIBILITY_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void shouldReturnAbleToVoteWhenExternalServiceApprovesCpf() {
        wireMock.stubFor(get(urlEqualTo(USER_INFO_PATH))
            .willReturn(okJson("""
                {"status":"ABLE_TO_VOTE"}
                """)));

        AssociateVotingStatus status = associateEligibilityPort.check(Cpf.from(CPF_WITH_MASK));

        assertEquals(AssociateVotingStatus.ABLE_TO_VOTE, status);
        verify(exactly(1), getRequestedFor(urlEqualTo(USER_INFO_PATH)));
    }

    @Test
    void shouldCacheEligibilityByCpf() {
        wireMock.stubFor(get(urlEqualTo(USER_INFO_PATH))
            .willReturn(okJson("""
                {"status":"ABLE_TO_VOTE"}
                """)));

        AssociateVotingStatus firstStatus = associateEligibilityPort.check(Cpf.from(CPF_WITH_MASK));
        AssociateVotingStatus secondStatus = associateEligibilityPort.check(Cpf.from(CPF_WITH_MASK));

        assertEquals(AssociateVotingStatus.ABLE_TO_VOTE, firstStatus);
        assertEquals(AssociateVotingStatus.ABLE_TO_VOTE, secondStatus);
        verify(exactly(1), getRequestedFor(urlEqualTo(USER_INFO_PATH)));
    }

    @Test
    void shouldNotUseRawCpfAsCacheKey() {
        wireMock.stubFor(get(urlEqualTo(USER_INFO_PATH))
            .willReturn(okJson("""
                {"status":"ABLE_TO_VOTE"}
                """)));

        associateEligibilityPort.check(Cpf.from(CPF_WITH_MASK));

        Cache cache = cacheManager.getCache(ASSOCIATE_ELIGIBILITY_CACHE);
        assertNotNull(cache);
        assertNull(cache.get(CPF));
        assertNotNull(cache.get(cpfHashingService.hash(Cpf.from(CPF_WITH_MASK))));
    }

    @Test
    void shouldFallbackToUnableToVoteWhenExternalServiceReturnsNotFound() {
        wireMock.stubFor(get(urlEqualTo(USER_INFO_PATH))
            .willReturn(aResponse().withStatus(404)));

        AssociateVotingStatus status = associateEligibilityPort.check(Cpf.from(CPF_WITH_MASK));

        assertEquals(AssociateVotingStatus.UNABLE_TO_VOTE, status);
        verify(exactly(1), getRequestedFor(urlEqualTo(USER_INFO_PATH)));
    }

    @Test
    void shouldRetryRetryableErrorsBeforeReturningSuccess() {
        wireMock.stubFor(get(urlEqualTo(USER_INFO_PATH))
            .inScenario("retry-success")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(serverError())
            .willSetStateTo("second-call"));

        wireMock.stubFor(get(urlEqualTo(USER_INFO_PATH))
            .inScenario("retry-success")
            .whenScenarioStateIs("second-call")
            .willReturn(okJson("""
                {"status":"ABLE_TO_VOTE"}
                """)));

        AssociateVotingStatus status = associateEligibilityPort.check(Cpf.from(CPF_WITH_MASK));

        assertEquals(AssociateVotingStatus.ABLE_TO_VOTE, status);
        verify(exactly(2), getRequestedFor(urlEqualTo(USER_INFO_PATH)));
    }

    @Test
    void shouldUseFallbackAndFailClosedWhenExternalServiceIsUnavailable() {
        wireMock.stubFor(get(urlEqualTo(USER_INFO_PATH))
            .willReturn(serviceUnavailable()));

        assertThrows(
            AssociateEligibilityUnavailableException.class,
            () -> associateEligibilityPort.check(Cpf.from(CPF_WITH_MASK))
        );
        verify(exactly(2), getRequestedFor(urlEqualTo(USER_INFO_PATH)));
    }

    @Test
    void shouldFailClosedWhenExternalServiceReturnsInvalidStatus() {
        wireMock.stubFor(get(urlEqualTo(USER_INFO_PATH))
            .willReturn(okJson("""
                {"status":"UNKNOWN"}
                """)));

        assertThrows(
            AssociateEligibilityUnavailableException.class,
            () -> associateEligibilityPort.check(Cpf.from(CPF_WITH_MASK))
        );
        verify(exactly(1), getRequestedFor(urlEqualTo(USER_INFO_PATH)));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
    })
    @Import({
        UserInfoClientConfiguration.class,
        IdentityHashConfiguration.class,
        UserInfoAssociateEligibilityAdapter.class,
        UserInfoClientMapper.class,
        UserInfoFeignClientFallbackFactory.class,
        CpfHashingService.class
    })
    static class TestApplication {
    }
}
