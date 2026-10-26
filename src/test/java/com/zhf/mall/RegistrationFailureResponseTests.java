package com.zhf.mall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zhf.mall.controller.UserController;
import com.zhf.mall.common.GlobalExceptionHandler;
import com.zhf.mall.common.ResultCode;
import com.zhf.mall.service.UserService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import jakarta.annotation.Resource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 验证内部故障的 HTTP 状态和三字段返回体，避免泄露敏感信息。 */
@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class RegistrationFailureResponseTests {

    @Resource
    private MockMvc mvc;

    @Resource
    private ObjectMapper json;

    @MockitoBean
    private UserService service;

    @Test
    void databaseOutageReturns503WithoutLeakingInternalDetails() throws Exception {
        when(service.register(any())).thenThrow(new DataAccessResourceFailureException("private database details"));
        assertSafeFailure(ResultCode.SERVICE_UNAVAILABLE);
    }

    @Test
    void programmingErrorReturns500WithoutLeakingInternalDetails() throws Exception {
        when(service.register(any())).thenThrow(new IllegalStateException("private internal details"));
        assertSafeFailure(ResultCode.INTERNAL_ERROR);
    }

    @Test
    void sqlProgrammingErrorIsNotReportedAsDatabaseOutage() throws Exception {
        when(service.register(any())).thenThrow(new BadSqlGrammarException(
                "private task", "private SQL", new java.sql.SQLException("private database details")));
        assertSafeFailure(ResultCode.INTERNAL_ERROR);
    }

    private void assertSafeFailure(ResultCode resultCode) throws Exception {
        var result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test_user\",\"password\":\"example123\"}"))
                .andExpect(status().is(resultCode.getCode()))
                .andExpect(jsonPath("$.code").value(resultCode.getCode()))
                .andExpect(jsonPath("$.msg").value(resultCode.getMsg()))
                .andReturn();
        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(response).contains("\"data\":null").doesNotContain("private", "example123", "SQLException");
        var body = json.readTree(response);
        assertThat(body.size()).isEqualTo(3);
        assertThat(body.has("code") && body.has("msg") && body.has("data")).isTrue();
    }
}
