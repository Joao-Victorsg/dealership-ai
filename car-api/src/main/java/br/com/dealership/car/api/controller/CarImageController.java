package br.com.dealership.car.api.controller;

import br.com.dealership.car.api.dto.request.PresignedUrlRequest;
import br.com.dealership.car.api.dto.response.PresignedUrlResponse;
import br.com.dealership.car.api.dto.response.Response;
import br.com.dealership.car.api.service.CarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cars")
@Tag(name = "Car Images", description = "Car image management via S3 presigned URLs")
public class CarImageController {

    private final CarService carService;

    public CarImageController(CarService carService) {
        this.carService = carService;
    }

    @PostMapping("/{id}/image/presigned-url")
    @Operation(
            summary = "Generate a presigned PUT URL for car image upload",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Presigned URL generated")
    @ApiResponse(responseCode = "400", description = "Invalid content type")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Car not found")
    public ResponseEntity<Response<PresignedUrlResponse>> getPresignedUrl(
            @PathVariable UUID id,
            @Valid @RequestBody PresignedUrlRequest request
    ) {
        return ResponseEntity.ok(Response.of(carService.generatePresignedUploadUrl(id, request.contentType())));
    }
}
