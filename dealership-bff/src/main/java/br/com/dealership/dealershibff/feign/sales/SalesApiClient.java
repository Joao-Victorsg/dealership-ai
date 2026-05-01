package br.com.dealership.dealershibff.feign.sales;

import br.com.dealership.dealershibff.feign.sales.dto.SalesApiPageResponse;
import br.com.dealership.dealershibff.feign.sales.dto.SalesApiRegisterRequest;
import br.com.dealership.dealershibff.feign.sales.dto.SalesApiSaleResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(
        name = "sales-api",
        url = "${spring.cloud.openfeign.client.config.sales-api.url}",
        configuration = SalesApiFeignConfig.class
)
public interface SalesApiClient {

    @PostMapping("/api/v1/sales")
    SalesApiSaleResponse registerSale(@RequestBody SalesApiRegisterRequest body);

    @GetMapping("/api/v1/sales/me")
    SalesApiPageResponse<SalesApiSaleResponse> listSales(
            @RequestHeader("Authorization") String token,
            @SpringQueryMap Map<String, Object> params);
}
