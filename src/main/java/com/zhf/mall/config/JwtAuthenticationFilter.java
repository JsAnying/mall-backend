package com.zhf.mall.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhf.mall.common.Result;
import com.zhf.mall.common.ResultCode;
import com.zhf.mall.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

/** 对携带的 JWT 验签并检查撤销状态，数据库故障时拒绝认证。 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public JwtAuthenticationFilter(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /**
     * 对携带的 JWT 验签并检查撤销状态，数据库故障时拒绝认证。
     * @param request 请求
     * @param response 响应
     * @param chain 过滤器链
     * @throws ServletException 如果过滤器链抛出
     * @throws IOException 如果输入输出错误
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        // 没有携带 JWT
        if (header == null) {
            chain.doFilter(request, response);
            return;
        }
        // 携带的 JWT 格式错误
        if (!header.startsWith("Bearer ") || header.substring(7).isBlank()) {
            writeError(response, ResultCode.UNAUTHORIZED);
            return;
        }
        // 验签失败
        try {
            Claims claims = JwtUtil.verify(header.substring(7));
            // 检查撤销状态
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM revoked_token WHERE jti = ?", Integer.class, claims.getId());
            // 撤销状态未知
            if (count == null || count > 0) {
                writeError(response, ResultCode.UNAUTHORIZED);
                return;
            }
            // 设置认证信息
            var authentication = new UsernamePasswordAuthenticationToken(claims.getSubject(), null, List.of());
            authentication.setDetails(claims);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException ex) {
            // JWT 格式错误
            writeError(response, ResultCode.UNAUTHORIZED);
            return;
            // 数据库故障
        } catch (DataAccessException ex) {
            writeError(response, ResultCode.SERVICE_UNAVAILABLE);
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * 向响应中写入错误结果。
     * @param response 响应
     * @param code 状态码
     * @throws IOException 如果输入输出错误
     */
    private void writeError(HttpServletResponse response, ResultCode code) throws IOException {
        response.setStatus(code.getCode());
        response.setContentType("application/json;charset=UTF-8");
        json.writeValue(response.getOutputStream(), Result.error(code));
    }
}
