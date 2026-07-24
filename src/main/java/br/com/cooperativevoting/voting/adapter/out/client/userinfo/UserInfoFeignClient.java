package br.com.cooperativevoting.voting.adapter.out.client.userinfo;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "user-info",
    url = "${integrations.user-info.base-url}",
    configuration = UserInfoFeignConfiguration.class,
    fallbackFactory = UserInfoFeignClientFallbackFactory.class
)
public interface UserInfoFeignClient {

    @GetMapping("/users/{cpf}")
    UserInfoClientResponse findByCpf(@PathVariable("cpf") String cpf);
}
