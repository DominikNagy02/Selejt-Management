package hu.unideb.inf.backend.controller;

import hu.unideb.inf.core.enums.TaskStatus;
import hu.unideb.inf.core.exception.InsufficientStockException;
import hu.unideb.inf.core.exception.InvalidTaskStateException;
import hu.unideb.inf.core.exception.ReservationConflictException;
import hu.unideb.inf.core.exception.ResourceNotFoundException;
import hu.unideb.inf.core.exception.ShelfCapacityExceededException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestErrorController.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestErrorController.class, GlobalExceptionHandlerTest.FixedClockConfig.class})
class GlobalExceptionHandlerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-10-20T14:30:00Z");

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock testClock() {
            return Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        }
    }

    @Test
    void mapsShelfCapacityExceededTo409ProblemDetailWithViolations() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/inbound"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/shelf-capacity-exceeded"))
                .andExpect(jsonPath("$.title").value("Shelf Capacity Exceeded"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(
                        "A polc (S-04-12) teherbírása meghaladná a maximális 500 kg-ot. Számított új súly: 540 kg."
                ))
                .andExpect(jsonPath("$.instance").value("/api/v1/inventory/inbound"))
                .andExpect(jsonPath("$.timestamp").value("2026-10-20T14:30:00Z"))
                .andExpect(jsonPath("$.shelfCode").value("S-04-12"))
                .andExpect(jsonPath("$.violations[0].dimension").value("weight"))
                .andExpect(jsonPath("$.violations[0].unit").value("kg"));
    }

    @Test
    void mapsReservationConflictTo409ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/tasks/reserve"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/reservation-conflict"))
                .andExpect(jsonPath("$.title").value("Reservation Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(
                        "A készlettétel (#884) nem foglalható le: kért mennyiség 20, még elérhető 5."
                ))
                .andExpect(jsonPath("$.instance").value("/api/v1/tasks/reserve"))
                .andExpect(jsonPath("$.timestamp").value("2026-10-20T14:30:00Z"));
    }

    @Test
    void mapsInsufficientStockTo409ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/transfer"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/insufficient-stock"))
                .andExpect(jsonPath("$.title").value("Insufficient Stock"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(
                        "Nincs elegendő elérhető készlet a termékből (SKU-100). Kért mennyiség: 50, elérhető: 12."
                ))
                .andExpect(jsonPath("$.instance").value("/api/v1/inventory/transfer"))
                .andExpect(jsonPath("$.timestamp").value("2026-10-20T14:30:00Z"));
    }

    @Test
    void mapsResourceNotFoundTo404ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/shelves/45"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/resource-not-found"))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(
                        "A keresett Shelf erőforrás nem található (azonosító: 45)."
                ))
                .andExpect(jsonPath("$.instance").value("/api/v1/shelves/45"))
                .andExpect(jsonPath("$.timestamp").value("2026-10-20T14:30:00Z"));
    }

    @Test
    void mapsInvalidTaskStateTo409ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/tasks/12/accept"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/invalid-task-state"))
                .andExpect(jsonPath("$.title").value("Invalid Task State"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(
                        "A feladat (#12) COMPLETED állapotból nem vihető IN_PROGRESS állapotba."
                ))
                .andExpect(jsonPath("$.instance").value("/api/v1/tasks/12/accept"))
                .andExpect(jsonPath("$.timestamp").value("2026-10-20T14:30:00Z"));
    }

    @Test
    void mapsMethodArgumentNotValidTo400ProblemDetailWithFieldErrors() throws Exception {
        String invalidJson = """
                {
                  "batchNumber": "",
                  "quantity": -5
                }
                """;

        mockMvc.perform(post("/api/v1/inventory/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/validation-failed"))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("A kérés adatai érvénytelenek."))
                .andExpect(jsonPath("$.instance").value("/api/v1/inventory/validate"))
                .andExpect(jsonPath("$.timestamp").value("2026-10-20T14:30:00Z"))
                .andExpect(jsonPath("$.errors.batchNumber").isNotEmpty())
                .andExpect(jsonPath("$.errors.quantity").isNotEmpty());
    }

    @Test
    void mapsMalformedJsonTo400ProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{broken-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/malformed-request"))
                .andExpect(jsonPath("$.title").value("Malformed Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("A kérés törzse nem olvasható vagy hibás JSON formátumú."))
                .andExpect(jsonPath("$.instance").value("/api/v1/inventory/validate"))
                .andExpect(jsonPath("$.timestamp").value("2026-10-20T14:30:00Z"));
    }

    @Test
    void mapsUnexpectedExceptionTo500ProblemDetailWithoutLeakingDetails() throws Exception {
        mockMvc.perform(get("/api/v1/unexpected-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("https://smartwms.hu/errors/internal-error"))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("Váratlan szerverhiba történt."))
                .andExpect(jsonPath("$.instance").value("/api/v1/unexpected-error"))
                .andExpect(jsonPath("$.timestamp").value("2026-10-20T14:30:00Z"));
    }

    @Test
    void fallsBackToDefaultMessageWhenFieldErrorMessageIsNull() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler(Clock.systemUTC());
        assertThrows(NullPointerException.class, () -> new GlobalExceptionHandler(null));

        TestPayload target = new TestPayload("", 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "testPayload");
        bindingResult.addError(new FieldError("testPayload", "batchNumber", null));

        Method method = TestErrorController.class.getDeclaredMethod("validate", TestPayload.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/inventory/validate");
        ProblemDetail problemDetail = handler.handleValidationErrors(exception, request);

        assertEquals(400, problemDetail.getStatus());
        assertNotNull(problemDetail.getProperties());
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) problemDetail.getProperties().get("errors");
        assertEquals("Invalid value", errors.get("batchNumber"));
    }

    @RestController
    static class TestErrorController {

        @PostMapping("/api/v1/inventory/inbound")
        public void inboundCapacityExceeded() {
            throw new ShelfCapacityExceededException(
                    "S-04-12",
                    new BigDecimal("500"),
                    new BigDecimal("540"),
                    "kg"
            );
        }

        @PostMapping("/api/v1/tasks/reserve")
        public void reservationConflict() {
            throw new ReservationConflictException(884L, 20, 5);
        }

        @PostMapping("/api/v1/inventory/transfer")
        public void insufficientStock() {
            throw new InsufficientStockException("SKU-100", 50, 12);
        }

        @GetMapping("/api/v1/shelves/{id}")
        public void shelfNotFound(@PathVariable("id") Long id) {
            throw ResourceNotFoundException.of("Shelf", id);
        }

        @PostMapping("/api/v1/tasks/{id}/accept")
        public void invalidTaskState(@PathVariable("id") Long id) {
            throw new InvalidTaskStateException(id, TaskStatus.COMPLETED, TaskStatus.IN_PROGRESS);
        }

        @PostMapping("/api/v1/inventory/validate")
        public void validate(@Valid @RequestBody TestPayload payload) {
            // Validated by Spring MVC before method body executes
        }

        @GetMapping("/api/v1/unexpected-error")
        public void unexpectedError() {
            throw new IllegalStateException("Sensitive internal database stack trace detail");
        }
    }

    record TestPayload(
            @NotBlank String batchNumber,
            @Positive int quantity
    ) {
    }
}
