package br.com.dealership.clientapi.client;

import br.com.dealership.clientapi.client.dto.ViaCepResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "viacep", url = "${app.viacep.base-url}")
public interface ViaCepFeignClient {

    @GetMapping("/ws/{postcode}/json/")
    ViaCepResponse lookupPostcode(@PathVariable("postcode") String postcode);
}
