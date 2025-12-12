package com.larlew.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.larlew.config.SecurityConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(MathController.class)
@Import(SecurityConfig.class)
public class MathControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sumEndpointReturnsCorrectResult() throws Exception {
        mockMvc.perform(get("/math/sum")
                .param("a", "5")
                .param("b", "7"))
                .andExpect(status().isOk())
                .andExpect(content().string("12"));
    }

    // Resiliency Tests

    @Test
    void sumEndpointHandlesNegativeNumbers() throws Exception {
        mockMvc.perform(get("/math/sum")
                .param("a", "-10")
                .param("b", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string("-5"));
    }

    @Test
    void sumEndpointHandlesZero() throws Exception {
        mockMvc.perform(get("/math/sum")
                .param("a", "0")
                .param("b", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    void sumEndpointHandlesLargeNumbers() throws Exception {
        mockMvc.perform(get("/math/sum")
                .param("a", String.valueOf(Integer.MAX_VALUE))
                .param("b", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string(String.valueOf(Integer.MAX_VALUE)));
    }

    @Test
    void sumEndpointHandlesOverflow() throws Exception {
        // Test integer overflow behavior
        mockMvc.perform(get("/math/sum")
                .param("a", String.valueOf(Integer.MAX_VALUE))
                .param("b", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(String.valueOf(Integer.MIN_VALUE)));
    }

    @Test
    void sumEndpointHandlesUnderflow() throws Exception {
        // Test integer underflow behavior
        mockMvc.perform(get("/math/sum")
                .param("a", String.valueOf(Integer.MIN_VALUE))
                .param("b", "-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(String.valueOf(Integer.MAX_VALUE)));
    }

    @Test
    void sumEndpointRejectsMissingParameter() throws Exception {
        mockMvc.perform(get("/math/sum")
                .param("a", "5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sumEndpointRejectsInvalidNumberFormat() throws Exception {
        mockMvc.perform(get("/math/sum")
                .param("a", "invalid")
                .param("b", "5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sumEndpointRejectsDecimalNumbers() throws Exception {
        mockMvc.perform(get("/math/sum")
                .param("a", "5.5")
                .param("b", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sumEndpointRejectsEmptyParameters() throws Exception {
        mockMvc.perform(get("/math/sum")
                .param("a", "")
                .param("b", "5"))
                .andExpect(status().isBadRequest());
    }
}
