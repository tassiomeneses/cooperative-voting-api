package br.com.cooperativevoting.voting.adapter.in.mobile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MobileCallbackUrlBuilder {

    private final String callbackBaseUrl;

    public MobileCallbackUrlBuilder(@Value("${mobile.callback-base-url:}") String callbackBaseUrl) {
        this.callbackBaseUrl = normalizeBaseUrl(callbackBaseUrl);
    }

    public String path(String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        if (!StringUtils.hasText(callbackBaseUrl)) {
            return normalizedPath;
        }
        return callbackBaseUrl + normalizedPath;
    }

    private String normalizeBaseUrl(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.replaceAll("/+$", "");
    }
}
