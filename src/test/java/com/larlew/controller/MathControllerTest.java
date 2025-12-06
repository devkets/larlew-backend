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
}
