package com.atguigu.daijia.common.login;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Description: 登录校验, 根据请求头中的token从redis中取出用户id, 并将用户id存入ThreadLocal中
 * @Author: Dong
 * @Date: 2024/7/26
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginAuth {



}
