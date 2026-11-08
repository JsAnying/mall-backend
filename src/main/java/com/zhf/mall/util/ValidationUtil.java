package com.zhf.mall.util;

import com.zhf.mall.common.ResultCode;
import com.zhf.mall.exception.BusinessException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

/** 参数校验工具，统一把 Bean Validation 约束转换为业务异常。 */
@Component
public class ValidationUtil {

    private final Validator validator;

    public ValidationUtil(Validator validator) {
        this.validator = validator;
    }

    /**
     * 校验命令对象，取字典序最小的违规提示作为错误信息。
     *
     * @param command 待校验对象
     */
    public void validate(Object command) {
        if (command == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请求参数不能为空");
        }
        validator.validate(command).stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .findFirst()
                .ifPresent(msg -> {
                    throw new BusinessException(ResultCode.BAD_REQUEST, msg);
                });
    }
}
