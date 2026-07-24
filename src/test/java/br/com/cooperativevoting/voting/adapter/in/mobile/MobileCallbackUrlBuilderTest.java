package br.com.cooperativevoting.voting.adapter.in.mobile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MobileCallbackUrlBuilderTest {

    @Test
    void shouldReturnRelativePathWhenBaseUrlIsBlank() {
        MobileCallbackUrlBuilder builder = new MobileCallbackUrlBuilder("");

        assertThat(builder.path("v1/mobile/pautas")).isEqualTo("/v1/mobile/pautas");
    }

    @Test
    void shouldPrefixConfiguredBaseUrlWithoutDuplicatingSlash() {
        MobileCallbackUrlBuilder builder = new MobileCallbackUrlBuilder("https://api.example.com/");

        assertThat(builder.path("/v1/mobile/pautas")).isEqualTo("https://api.example.com/v1/mobile/pautas");
    }
}
