package br.com.dealership.car.api.controller;
import br.com.dealership.car.api.config.GlobalExceptionHandler;
import br.com.dealership.car.api.domain.enums.CarCategory;
import br.com.dealership.car.api.domain.enums.CarStatus;
import br.com.dealership.car.api.domain.enums.PropulsionType;
import br.com.dealership.car.api.domain.exception.CarNotFoundException;
import br.com.dealership.car.api.domain.exception.DuplicateVinException;
import br.com.dealership.car.api.domain.exception.SoldCarModificationException;
import br.com.dealership.car.api.dto.request.CarFilterRequest;
import br.com.dealership.car.api.dto.request.CreateCarRequest;
import br.com.dealership.car.api.dto.request.UpdateCarRequest;
import br.com.dealership.car.api.dto.response.CarResponse;
import br.com.dealership.car.api.service.CarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@ExtendWith(MockitoExtension.class)
class CarControllerTest {
    @Mock
    private CarService carService;
    @InjectMocks
    private CarController carController;
    private MockMvc mockMvc;
    private static final String VALID_VIN = "ABCDE1234567890AB";
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(carController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }
    private CarResponse sampleCarResponse(UUID id) {
        return CarResponse.builder()
                .id(id)
                .model("Tesla Model 3")
                .manufacturingYear(2022)
                .manufacturer("Tesla")
                .externalColor("White")
                .internalColor("Black")
                .vin(VALID_VIN)
                .status(CarStatus.AVAILABLE)
                .category(CarCategory.SEDAN)
                .kilometers(BigDecimal.valueOf(10000))
                .isNew(false)
                .propulsionType(PropulsionType.ELECTRIC)
                .listedValue(BigDecimal.valueOf(50000))
                .registrationDate(Instant.now())
                .build();
    }
    @Test
    void shouldRegisterCarAndReturnCreated() throws Exception {
        var carId = UUID.randomUUID();
        var carResponse = sampleCarResponse(carId);
        when(carService.registerCar(any(CreateCarRequest.class))).thenReturn(carResponse);
        var requestBody = """
                {
                  "model": "Tesla Model 3",
                  "manufacturingYear": 2022,
                  "manufacturer": "Tesla",
                  "externalColor": "White",
                  "internalColor": "Black",
                  "vin": "ABCDE1234567890AB",
                  "status": "AVAILABLE",
                  "category": "SEDAN",
                  "kilometers": 10000,
                  "isNew": false,
                  "propulsionType": "ELECTRIC",
                  "listedValue": 50000
                }
                """;
        mockMvc.perform(post("/api/v1/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.vin").value(VALID_VIN))
                .andExpect(header().exists("Location"));
    }
    @Test
    void shouldReturnConflictWhenVinIsDuplicated() throws Exception {
        when(carService.registerCar(any(CreateCarRequest.class))).thenThrow(new DuplicateVinException(VALID_VIN));
        var requestBody = """
                {
                  "model": "Tesla Model 3",
                  "manufacturingYear": 2022,
                  "manufacturer": "Tesla",
                  "externalColor": "White",
                  "internalColor": "Black",
                  "vin": "ABCDE1234567890AB",
                  "status": "AVAILABLE",
                  "category": "SEDAN",
                  "kilometers": 10000,
                  "isNew": false,
                  "propulsionType": "ELECTRIC",
                  "listedValue": 50000
                }
                """;
        mockMvc.perform(post("/api/v1/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict());
    }
    @Test
    void shouldReturnBadRequestWhenRequestBodyIsInvalid() throws Exception {
        var invalidBody = """
                {
                  "model": "",
                  "manufacturingYear": null,
                  "manufacturer": ""
                }
                """;
        mockMvc.perform(post("/api/v1/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }
    @Test
    void shouldListCarsSuccessfully() throws Exception {
        var carId = UUID.randomUUID();
        var carResponse = sampleCarResponse(carId);
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(carResponse), pageable, 1);
        when(carService.listCars(any(CarFilterRequest.class), any())).thenReturn(page);
        mockMvc.perform(get("/api/v1/cars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].vin").value(VALID_VIN));
    }
    @Test
    void shouldReturnEmptyPageWhenNoCarsMatch() throws Exception {
        var pageable = PageRequest.of(0, 20);
        var emptyPage = new PageImpl<CarResponse>(List.of(), pageable, 0);
        when(carService.listCars(any(CarFilterRequest.class), any())).thenReturn(emptyPage);
        mockMvc.perform(get("/api/v1/cars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }
    @Test
    void shouldGetCarByIdSuccessfully() throws Exception {
        var carId = UUID.randomUUID();
        var carResponse = sampleCarResponse(carId);
        when(carService.getCarById(carId)).thenReturn(carResponse);
        mockMvc.perform(get("/api/v1/cars/" + carId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(carId.toString()))
                .andExpect(jsonPath("$.data.vin").value(VALID_VIN));
    }
    @Test
    void shouldReturnNotFoundWhenCarDoesNotExist() throws Exception {
        var carId = UUID.randomUUID();
        when(carService.getCarById(carId)).thenThrow(new CarNotFoundException(carId));
        mockMvc.perform(get("/api/v1/cars/" + carId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.message").exists());
    }
    @Test
    void shouldUpdateCarSuccessfully() throws Exception {
        var carId = UUID.randomUUID();
        var updatedResponse = CarResponse.builder()
                .id(carId).model("Tesla Model 3").manufacturingYear(2022).manufacturer("Tesla")
                .externalColor("White").internalColor("Black").vin(VALID_VIN)
                .status(CarStatus.UNAVAILABLE).category(CarCategory.SEDAN)
                .kilometers(BigDecimal.valueOf(10000)).isNew(false)
                .propulsionType(PropulsionType.ELECTRIC).listedValue(BigDecimal.valueOf(50000))
                .registrationDate(Instant.now()).build();
        when(carService.updateCar(eq(carId), any(UpdateCarRequest.class))).thenReturn(updatedResponse);
        mockMvc.perform(patch("/api/v1/cars/" + carId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"UNAVAILABLE\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNAVAILABLE"));
    }
    @Test
    void shouldReturnNotFoundWhenUpdatingNonexistentCar() throws Exception {
        var carId = UUID.randomUUID();
        when(carService.updateCar(eq(carId), any(UpdateCarRequest.class))).thenThrow(new CarNotFoundException(carId));
        mockMvc.perform(patch("/api/v1/cars/" + carId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"UNAVAILABLE\" }"))
                .andExpect(status().isNotFound());
    }
    @Test
    void shouldReturnUnprocessableEntityWhenUpdatingSoldCar() throws Exception {
        var carId = UUID.randomUUID();
        when(carService.updateCar(eq(carId), any(UpdateCarRequest.class))).thenThrow(new SoldCarModificationException());
        mockMvc.perform(patch("/api/v1/cars/" + carId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"AVAILABLE\" }"))
                .andExpect(status().isUnprocessableContent());
    }
}
