package br.com.dealership.car.api.controller;
import br.com.dealership.car.api.config.GlobalExceptionHandler;
import br.com.dealership.car.api.domain.exception.CarNotFoundException;
import br.com.dealership.car.api.dto.response.PresignedUrlResponse;
import br.com.dealership.car.api.service.CarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@ExtendWith(MockitoExtension.class)
class CarImageControllerTest {
    @Mock
    private CarService carService;
    @InjectMocks
    private CarImageController carImageController;
    private MockMvc mockMvc;
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(carImageController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
    @Test
    void shouldReturnPresignedUrlForJpegContentType() throws Exception {
        var carId = UUID.randomUUID();
        var presignedResponse = PresignedUrlResponse.of("https://s3.amazonaws.com/bucket/cars/image.jpg", "cars/" + carId + "/image.jpg", 3600);
        when(carService.generatePresignedUploadUrl(eq(carId), eq("image/jpeg"))).thenReturn(presignedResponse);
        mockMvc.perform(post("/api/v1/cars/" + carId + "/image/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"contentType\": \"image/jpeg\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.presignedUrl").exists())
                .andExpect(jsonPath("$.data.objectKey").exists())
                .andExpect(jsonPath("$.data.expiresIn").value(3600));
    }
    @Test
    void shouldReturnPresignedUrlForPngContentType() throws Exception {
        var carId = UUID.randomUUID();
        var presignedResponse = PresignedUrlResponse.of("https://s3.amazonaws.com/bucket/cars/image.png", "cars/" + carId + "/image.png", 900);
        when(carService.generatePresignedUploadUrl(eq(carId), eq("image/png"))).thenReturn(presignedResponse);
        mockMvc.perform(post("/api/v1/cars/" + carId + "/image/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"contentType\": \"image/png\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.presignedUrl").exists());
    }
    @Test
    void shouldReturnPresignedUrlForWebpContentType() throws Exception {
        var carId = UUID.randomUUID();
        var presignedResponse = PresignedUrlResponse.of("https://s3.amazonaws.com/bucket/cars/image.webp", "cars/image.webp", 3600);
        when(carService.generatePresignedUploadUrl(eq(carId), eq("image/webp"))).thenReturn(presignedResponse);
        mockMvc.perform(post("/api/v1/cars/" + carId + "/image/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"contentType\": \"image/webp\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.presignedUrl").exists());
    }
    @Test
    void shouldReturnNotFoundWhenCarDoesNotExist() throws Exception {
        var carId = UUID.randomUUID();
        when(carService.generatePresignedUploadUrl(eq(carId), any())).thenThrow(new CarNotFoundException(carId));
        mockMvc.perform(post("/api/v1/cars/" + carId + "/image/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"contentType\": \"image/jpeg\" }"))
                .andExpect(status().isNotFound());
    }
    @Test
    void shouldReturnBadRequestWhenContentTypeIsUnsupported() throws Exception {
        var carId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/cars/" + carId + "/image/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"contentType\": \"application/pdf\" }"))
                .andExpect(status().isBadRequest());
    }
    @Test
    void shouldReturnBadRequestWhenContentTypeIsBlank() throws Exception {
        var carId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/cars/" + carId + "/image/presigned-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"contentType\": \"\" }"))
                .andExpect(status().isBadRequest());
    }
}
