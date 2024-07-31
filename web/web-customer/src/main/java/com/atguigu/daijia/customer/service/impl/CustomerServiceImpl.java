package com.atguigu.daijia.customer.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.customer.service.CustomerService;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerInfoFeignClient customerInfoFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public String login(String code) {
        // 1. 远程调用
        Result<Long> loginResult = customerInfoFeignClient.login(code);

        // 2. 判断如果返回失败了, 返回错误信息
        if (loginResult.getCode() != 200){
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 3. 获取用户id
        Long customerId = loginResult.getData();

        // 4. 判断用户id是否为空, 若为空, 则返回错误信息
        if(customerId == null){
            throw new GuiguException(ResultCodeEnum.ARGUMENT_VALID_ERROR);
        }

        // 5. 生成token字符串
        String token = UUID.randomUUID().toString().replaceAll("-", "");

        // 6. 将用户id(key: token, value customerId)放入Redis中, 并设置过期时间
        // redisTemplate.opsForValue().set(token, customerId, 30, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX + token,
                customerId.toString(),
                RedisConstant.USER_LOGIN_KEY_TIMEOUT,
                TimeUnit.SECONDS);

        // 7. 返回token
        return token;
    }

    @Override
    public CustomerLoginVo getCustomerInfo(Long customerId) {

        if (customerId == null){
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);  // TODO: 跳转登录
        }

        Result<CustomerLoginVo> customerLoginResult =
                customerInfoFeignClient.getCustomerInfo(customerId);

        if (customerLoginResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        CustomerLoginVo customerLoginVo = customerLoginResult.getData();
        if (customerLoginVo == null){
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        return customerLoginVo;
    }

//    @Override
//    public CustomerLoginVo getCustomerLoginInfo(String token) {
//
//        String customerId = (String) redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);
//        if (!StringUtils.hasText(customerId)){
//            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
//        }
//
//        Result<CustomerLoginVo> customerLoginResult =
//                customerInfoFeignClient.getCustomerInfo(Long.parseLong(customerId));
//
//        if (customerLoginResult.getCode() != 200) {
//            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
//        }
//
//        CustomerLoginVo customerLoginVo = customerLoginResult.getData();
//        if (customerLoginVo == null){
//            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
//        }
//
//        return customerLoginVo;
//    }

    @Override
    public Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm) {
        Result<Boolean> result = customerInfoFeignClient.updateWxPhoneNumber(updateWxPhoneForm);
        if (result.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }

        if (result.getData() == null || !result.getData()){
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }

        return true;
    }

}
