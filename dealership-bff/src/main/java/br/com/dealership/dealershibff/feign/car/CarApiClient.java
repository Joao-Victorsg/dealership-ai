package br.com.dealership.dealershibff.feign.car;

import br.com.dealership.dealershibff.feign.car.dto.CarApiCarResponse;
import br.com.dealership.dealershibff.feign.car.dto.CarApiDataResponse;
import br.com.dealership.dealershibff.feign.car.dto.CarApiFilterParams;
import br.com.dealership.dealershibff.feign.car.dto.CarApiFilterOptionsResponse;
import br.com.dealership.dealershibff.feign.car.dto.CarApiPageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "car-api",
        url = "${spring.cloud.openfeign.client.config.car-api.url}",
        configuration = CarApiFeignConfig.class
)
public interface CarApiClient {

    @GetMapping("/api/v1/cars")
    CarApiDataResponse<CarApiPageResponse<CarApiCarResponse>> listCars(@SpringQueryMap CarApiFilterParams params);

    @GetMapping("/api/v1/cars/filter-options")
    CarApiDataResponse<CarApiFilterOptionsResponse> getFilterOptions();

    @GetMapping("/api/v1/cars/{id}")
    CarApiDataResponse<CarApiCarResponse> getCarById(@PathVariable UUID id);
}
