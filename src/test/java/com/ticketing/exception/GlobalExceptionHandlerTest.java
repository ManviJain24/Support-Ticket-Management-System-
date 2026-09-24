package com.ticketing.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest
@ContextConfiguration(classes = {
    GlobalExceptionHandler.class,
    GlobalExceptionHandlerTest.TestController.class
})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn404_whenTicketIsMissing() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Ticket not found"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void shouldReturn422_whenTransitionIsInvalid() throws Exception {
        mockMvc.perform(get("/test/invalid-transition"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    void shouldReturn400WithFieldDetails_whenBeanValidationFails() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("title"));
    }

    @Test
    void shouldReturn500WithStableBody_whenUnexpectedErrorOccurs() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/not-found")
        void notFound() {
            throw new AppException(ErrorCode.TICKET_NOT_FOUND, "Ticket not found");
        }

        @GetMapping("/invalid-transition")
        void invalidTransition() {
            throw new AppException(ErrorCode.INVALID_TRANSITION, "Invalid transition");
        }

        @PostMapping("/validate")
        void validate(@Valid @RequestBody TestRequest request) {}

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("Do not expose this message");
        }
    }

    record TestRequest(@NotBlank String title) {}
}
