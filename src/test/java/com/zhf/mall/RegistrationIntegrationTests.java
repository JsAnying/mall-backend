package com.zhf.mall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.zhf.mall.mapper.UserMapper;
import com.zhf.mall.dto.command.UserRegisterCommandDTO;
import com.zhf.mall.entity.User;
import com.zhf.mall.common.ResultCode;
import jakarta.validation.Validator;
import com.zhf.mall.exception.BusinessException;
import com.zhf.mall.service.impl.UserServiceImpl;
import com.zhf.mall.util.ValidationUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import jakarta.annotation.Resource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 验证注册接口、真实 Mapper 映射及三字段返回契约。 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationIntegrationTests {
    @Resource MockMvc mvc;
    @Resource ObjectMapper json;
    @Resource UserMapper mapper;
    @Resource PasswordEncoder encoder;
    @Resource JdbcTemplate jdbc;
    @Resource Validator validator;

    @BeforeEach
    void resetOnlyTheEmbeddedTestDatabase() throws Exception {
        try (var connection = jdbc.getDataSource().getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("H2");
        }
        jdbc.update("DELETE FROM `user`");
    }

    @Test
    void registerPersistsHashAndReturnsTheActualGeneratedId() throws Exception {
        String password = "  example123  ";
        LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1);
        MvcResult result = register("  Alice_01  ", password)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andExpect(jsonPath("$.data.id").isString())
                .andExpect(jsonPath("$.data.username").value("Alice_01"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(cookie().doesNotExist("JSESSIONID"))
                .andReturn();
        assertResponseFields(result);
        User saved = findUser("alice_01");
        assertThat(saved).isNotNull();
        assertThat(saved.getNickname()).isEqualTo("Alice_01");
        assertThat(saved.getPasswordHash()).isNotEqualTo(password).startsWith("$2");
        assertThat(encoder.matches(password, saved.getPasswordHash())).isTrue();
        assertThat(encoder.matches(password.trim(), saved.getPasswordHash())).isFalse();
        assertThat(saved.getCreatedAt()).isAfter(before).isBefore(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(1));
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(json.readTree(result.getResponse().getContentAsString()).at("/data/id").asText())
                .isEqualTo(saved.getId().toString());
        MvcResult second = register("second_user", "example123").andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(second.getResponse().getContentAsString()).at("/data/id").asText())
                .isNotEqualTo(saved.getId().toString());
    }

    @Test
    void duplicateUsernameIncludingCaseReturns409AndDoesNotCreateAnotherUser() throws Exception {
        register("Alice_01", "example123").andExpect(status().isOk());
        MvcResult result = register("  ALICE_01 ", "different123")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.msg").value(ResultCode.USERNAME_EXISTS.getMsg()))
                .andReturn();
        assertErrorEnvelope(result);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM `user`", Integer.class)).isEqualTo(1);
        assertThat(encoder.matches("example123", findUser("Alice_01").getPasswordHash())).isTrue();
    }

    @Test
    void simultaneousRegistrationsAreArbitratedByTheRealUniqueConstraint() throws Exception {
        CyclicBarrier bothQueriesCompleted = new CyclicBarrier(2);
        UserMapper racingMapper = mock(UserMapper.class, delegatesTo(mapper));
        doAnswer(invocation -> {
            Wrapper<User> query = invocation.getArgument(0);
            User saved = mapper.selectOne(query);
            bothQueriesCompleted.await(5, TimeUnit.SECONDS);
            return saved;
        }).when(racingMapper).selectOne(any(Wrapper.class));
        UserServiceImpl racingService = new UserServiceImpl();
        ReflectionTestUtils.setField(racingService, "userMapper", racingMapper);
        ReflectionTestUtils.setField(racingService, "passwordEncoder", encoder);
        ReflectionTestUtils.setField(racingService, "validationUtil", new ValidationUtil(validator));
        java.util.concurrent.Callable<Integer> register = () -> {
            try {
                racingService.register(new UserRegisterCommandDTO("racing_user", "example123"));
                return 200;
            } catch (BusinessException exception) {
                assertThat(exception.getResultCode()).isEqualTo(ResultCode.USERNAME_EXISTS);
                return exception.getResultCode().getCode();
            }
        };
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(register);
            var second = executor.submit(register);
            assertThat(java.util.List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        } finally {
            executor.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM `user`", Integer.class)).isEqualTo(1);
    }

    static Stream<Arguments> invalidCredentials() {
        return Stream.of(
                Arguments.of(null, "example123"), Arguments.of("", "example123"),
                Arguments.of("   ", "example123"), Arguments.of("ab", "example123"),
                Arguments.of("a".repeat(33), "example123"), Arguments.of("bad-name", "example123"),
                Arguments.of("中文用户名", "example123"), Arguments.of("user_01", null),
                Arguments.of("user_01", ""), Arguments.of("user_01", "      "), Arguments.of("user_01", "12345"),
                Arguments.of("user_01", "a".repeat(65)), Arguments.of("user_01", "密".repeat(25)));
    }

    @ParameterizedTest
    @MethodSource("invalidCredentials")
    void invalidFieldsReturn400AndNeverInsert(String username, String password) throws Exception {
        MvcResult result = register(username, password)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andReturn();
        assertErrorEnvelope(result);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM `user`", Integer.class)).isZero();
    }

    static Stream<Arguments> validBoundaries() {
        return Stream.of(Arguments.of("abc", "123456"),
                Arguments.of("a".repeat(32), "p".repeat(64)),
                Arguments.of("multibyte_user", "密".repeat(24)));
    }

    @ParameterizedTest
    @MethodSource("validBoundaries")
    void validLengthAnd72ByteBoundariesAreAccepted(String username, String password) throws Exception {
        register(username, password).andExpect(status().isOk());
        assertThat(encoder.matches(password, findUser(username).getPasswordHash())).isTrue();
    }

    @Test
    void missingFieldsMalformedJsonAndWrongContentTypeHaveJsonErrors() throws Exception {
        for (String body : new String[]{"{}", "null", "", "{bad json",
                "{\"username\":[],\"password\":\"example123\"}",
                "{\"username\":123,\"password\":\"example123\"}",
                "{\"username\":\"test_user\",\"password\":123456}",
                "{\"username\":\"test_user\",\"password\":true}"}) {
            MvcResult result = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andReturn();
            assertErrorEnvelope(result);
        }
        mvc.perform(post("/api/auth/register").contentType(MediaType.TEXT_PLAIN).content("test"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(415))
                .andExpect(jsonPath("$.msg").value(ResultCode.UNSUPPORTED_MEDIA_TYPE.getMsg()));
    }

    @Test
    void requestsReachMvcWithoutSecurityInterception() throws Exception {
        MvcResult result = mvc.perform(get("/api/users/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.msg").value(ResultCode.NOT_FOUND.getMsg())).andReturn();
        assertErrorEnvelope(result);
        mvc.perform(get("/api/auth/register"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(405));
    }

    @Test
    void passwordNeverAppearsInDtoLogsOrJson() throws Exception {
        UserRegisterCommandDTO request = new UserRegisterCommandDTO("test_user", "private-example123");
        assertThat(request.toString()).doesNotContain("private-example123");
        assertThat(json.writeValueAsString(request)).doesNotContain("private-example123", "passwordWithinByteLimit");
        User user = new User();
        user.setPasswordHash("private-hash");
        assertThat(user.toString()).doesNotContain("private-hash");
        assertThat(json.writeValueAsString(user)).doesNotContain("private-hash", "passwordHash");
    }

    private User findUser(String username) {
        return mapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Test
    void loginRemainsOutsideTheCurrentScope() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user_01\",\"password\":\"example123\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    private org.springframework.test.web.servlet.ResultActions register(String username, String password) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("password", password);
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8).content(json.writeValueAsString(body)));
    }

    /** 精确检查返回体字段，禁止旧字段或新增字段混入接口。 */
    private void assertResponseFields(MvcResult result) throws Exception {
        var body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.isObject()).isTrue();
        assertThat(body.size()).isEqualTo(3);
        assertThat(body.has("code") && body.has("msg") && body.has("data")).isTrue();
        assertThat(body.path("code").asInt()).isEqualTo(result.getResponse().getStatus());
        assertThat(body.path("msg").asText()).isNotBlank();
    }

    private void assertErrorEnvelope(MvcResult result) throws Exception {
        assertResponseFields(result);
        var body = json.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.get("data").isNull()).isTrue();
    }
}
