package br.com.dealership.dealershibff.feign.client;

import br.com.dealership.dealershibff.feign.client.dto.ClientApiClientResponse;
import br.com.dealership.dealershibff.feign.client.dto.ClientApiCreateRequest;
import br.com.dealership.dealershibff.feign.client.dto.ClientApiUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(
        name = "client-api",
        url = "${spring.cloud.openfeign.client.config.client-api.url}",
        configuration = ClientApiFeignConfig.class
)
public interface ClientApiClient {

    @GetMapping("/clients/me")
    ClientApiClientResponse getMe(@RequestHeader("Authorization") String bearerToken);

    @PostMapping("/clients")
    ClientApiClientResponse create(@RequestHeader("Authorization") String bearerToken,
                                   @RequestBody ClientApiCreateRequest body);

    @PatchMapping("/clients/{id}")
    ClientApiClientResponse update(
            @PathVariable UUID id,
            @RequestBody ClientApiUpdateRequest body
    );
}
