package br.com.dealership.car.api.controller;

import br.com.dealership.car.api.dto.request.CarFilterRequest;
import br.com.dealership.car.api.dto.response.CarResponse;
import br.com.dealership.car.api.dto.request.CreateCarRequest;
import br.com.dealership.car.api.dto.response.Response;
import br.com.dealership.car.api.dto.request.UpdateCarRequest;
import br.com.dealership.car.api.service.CarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cars")
@Tag(name = "Cars", description = "Car inventory management")
public class CarController {

    private final CarService carService;

    public CarController(CarService carService) {
        this.carService = carService;
    }

    @PostMapping
    @Operation(
            summary = "Register a new car",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "201", description = "Car registered successfully")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "409", description = "Duplicate VIN")
    public ResponseEntity<Response<CarResponse>> registerCar(@Valid @RequestBody CreateCarRequest request) {
        var car = carService.registerCar(request);
        var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(car.id())
                .toUri();
        return ResponseEntity.created(location).body(Response.of(car));
    }

    @GetMapping
    @Operation(summary = "List cars with optional filters and pagination")
    @ApiResponse(responseCode = "200", description = "Cars retrieved successfully")
    public ResponseEntity<Response<Page<CarResponse>>> listCars(
            @ModelAttribute CarFilterRequest filter,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        var result = carService.listCars(filter, pageable);
        return ResponseEntity.ok(Response.of(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get car by ID")
    @ApiResponse(responseCode = "200", description = "Car found")
    @ApiResponse(responseCode = "404", description = "Car not found")
    public ResponseEntity<Response<CarResponse>> getCarById(@PathVariable UUID id) {
        return ResponseEntity.ok(Response.of(carService.getCarById(id)));
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Update a car's mutable attributes",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Car updated successfully")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Car not found")
    @ApiResponse(responseCode = "422", description = "Car is sold and cannot be modified")
    public ResponseEntity<Response<CarResponse>> updateCar(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCarRequest request
    ) {
        return ResponseEntity.ok(Response.of(carService.updateCar(id, request)));
    }
}
