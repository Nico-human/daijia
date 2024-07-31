package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.DriverService;
import com.atguigu.daijia.map.client.LocationFeignClient;
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
    @Autowired
    private LocationFeignClient locationFeignClient;
    @Autowired
    private NewOrderFeignClient newOrderFeignClient;

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

    @Override
    public Boolean isFaceRecognition(Long driverId) {
        Result<Boolean> result = driverInfoFeignClient.isFaceRecognition(driverId);
        if (result.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }
        return result.getData();
    }

    @Override
    public Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm) {
        Result<Boolean> result = driverInfoFeignClient.verifyDriverFace(driverFaceModelForm);
        if (result.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }
        return result.getData();
    }

    @Override
    public Boolean startService(Long driverId) {

        // 1.是否完成了认证
        Result<DriverLoginVo> driverLoginVoResult = driverInfoFeignClient.getDriverLoginInfo(driverId);
        if (driverLoginVoResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }
        DriverLoginVo driverLoginVo = driverLoginVoResult.getData();
        if (driverLoginVo.getAuthStatus() != 2) {
            throw new GuiguException(ResultCodeEnum.AUTH_ERROR);
        }
        // 2.当日是否进行了人脸识别
        Result<Boolean> faceRecognitionResult = driverInfoFeignClient.isFaceRecognition(driverId);
        if (faceRecognitionResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }
        Boolean isRecognition = faceRecognitionResult.getData();
        if (!isRecognition) {
            throw new GuiguException(ResultCodeEnum.FACE_ERROR);
        }
        // 3.更新订单状态(1) 开始接单
        driverInfoFeignClient.updateServiceStatus(driverId, 1);

        // 4.删除redis中司机位置信息
        locationFeignClient.removeDriverLocation(driverId);

        // 5.清空司机临时队列数据
        newOrderFeignClient.clearNewOrderQueueData(driverId);

        return true;
    }

    @Override
    public Boolean stopService(Long driverId) {
        // 1.更新订单状态(0) 停止接单
        driverInfoFeignClient.updateServiceStatus(driverId, 1);

        // 2.删除redis中司机位置信息
        locationFeignClient.removeDriverLocation(driverId);

        // 3.清空司机临时队列数据
        newOrderFeignClient.clearNewOrderQueueData(driverId);

        return true;
    }


}
