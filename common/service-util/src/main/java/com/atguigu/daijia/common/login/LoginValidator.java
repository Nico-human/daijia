package com.atguigu.daijia.common.login;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @Description: 登录校验实现
 * @Author: Dong
 * @Date: 2024/7/26
 */

@Component
@Aspect
public class LoginValidator {

    @Autowired
    private RedisTemplate redisTemplate;

    @Around("execution(* com.atguigu.daijia.*.controller.*.*(..)) && @annotation(com.atguigu.daijia.common.login.LoginAuth)")
    public Object loginValidator(ProceedingJoinPoint joinPoint) throws Throwable {

        // 获取request对象
        ServletRequestAttributes servletRequestAttributes =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes());
        HttpServletRequest request = servletRequestAttributes.getRequest();
        // 获取请求头中的token, 并判断token是否为空
        String token = request.getHeader("token");
        if (!StringUtils.hasText(token)) {
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }
        // 查询redis对应用户id, 把用户id放到ThreadLocal里面
        String customerId = (String) redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);
        if(StringUtils.hasText(customerId)){
            AuthContextHolder.setUserId(Long.parseLong(customerId));
        }

        return joinPoint.proceed();
    }



}
