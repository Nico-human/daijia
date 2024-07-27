package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.DriverService;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverServiceImpl implements DriverService {

    @Autowired
    private DriverInfoFeignClient driverInfoFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public String login(String code) {
        Result<Long> result = driverInfoFeignClient.login(code);
        if(result.getCode() != 200){
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }

        Long driverId =  result.getData();
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        redisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX + token,
                                            driverId.toString(),
                                            RedisConstant.USER_LOGIN_REFRESH_KEY_TIMEOUT,
                                            TimeUnit.SECONDS);
        return token;
    }

    @Override
    public DriverLoginVo getDriverLoginInfo(Long driverId) {
        Result<DriverLoginVo> driverLoginInfo = driverInfoFeignClient.getDriverLoginInfo(driverId);
        if (driverLoginInfo.getCode() != 200){
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }
        return driverLoginInfo.getData();
    }

    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {
        Result<DriverAuthInfoVo> driverAuthInfo = driverInfoFeignClient.getDriverAuthInfo(driverId);
        if (driverAuthInfo.getCode() != 200){
            throw new GuiguException(ResultCodeEnum.AUTH_ERROR);
        }
        return driverAuthInfo.getData();
    }

    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        Result<Boolean> booleanResult = driverInfoFeignClient.updateDriverAuthInfo(updateDriverAuthInfoForm);
        if (booleanResult.getCode() != 200){
            return false;
        }
        return booleanResult.getData();
    }

    @Override
    public Boolean createDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
        Result<Boolean> driverFaceModel = driverInfoFeignClient.createDriverFaceModel(driverFaceModelForm);
        if (driverFaceModel.getCode() != 200) {
            return false;
        }
        return driverFaceModel.getData();
    }

}
