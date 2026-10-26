package com.zhf.mall.dto.command;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 注册请求参数，只接收用户名和密码。
 * 用户名在创建对象时标准化；密码保持原样，且不会出现在日志或 JSON 输出中。
 */
@Data
@NoArgsConstructor
public class UserRegisterCommandDTO {
    /** 3～32 位字母、数字或下划线，首尾空白会被移除。 */
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_]{3,32}$", message = "用户名必须为 3～32 位字母、数字或下划线")
    private String username;

    /** 密码长度为 6～64 位，UTF-8 最多 72 字节。 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度必须在 6 到 64 位之间")
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 显式指定 JSON 构造入口，保证标准化发生在 Bean Validation 校验之前。 */
    @JsonCreator
    public UserRegisterCommandDTO(@JsonProperty("username") String username,
                           @JsonProperty("password") String password) {
        setUsername(username);
        this.password = password;
    }

    // JSON 反序列化时先标准化用户名，再校验；密码不做 trim。
    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }

    /** BCrypt 输入存在字节上限，字符数校验不能代替此检查。 */
    @JsonIgnore
    @AssertTrue(message = "密码的 UTF-8 编码不能超过 72 字节")
    public boolean isPasswordWithinByteLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
