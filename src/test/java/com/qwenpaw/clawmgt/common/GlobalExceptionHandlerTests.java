package com.qwenpaw.clawmgt.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTests.TestController.class})
class GlobalExceptionHandlerTests {
    @Autowired
    MockMvc mockMvc;

    @Test
    void validationErrorReturnsBadRequestApiResponse() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("name")))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    @Test
    void businessExceptionReturnsConfiguredStatusApiResponse() throws Exception {
        mockMvc.perform(post("/test/business")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("TASK_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Task is already running"))
                .andExpect(jsonPath("$.data", nullValue()));
    }

    @RestController
    static class TestController {
        @PostMapping("/test/validate")
        ApiResponse<String> validate(@Valid @RequestBody TestRequest request) {
            return ApiResponse.success(request.name());
        }

        @PostMapping("/test/business")
        ApiResponse<Void> business() {
            throw new BusinessException(HttpStatus.CONFLICT, "TASK_CONFLICT", "Task is already running");
        }
    }

    record TestRequest(@NotBlank String name) {
    }
}
