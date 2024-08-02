package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.LocationService;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class LocationServiceImpl implements LocationService {

    @Autowired
    private LocationFeignClient locationFeignClient;

    @Autowired
    private DriverInfoFeignClient driverInfoFeignClient;

    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {

        Result<DriverSet> driverSetResult = driverInfoFeignClient.getDriverSet(updateDriverLocationForm.getDriverId());
        if (driverSetResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }
        DriverSet driverSet = driverSetResult.getData();

        if (driverSet.getServiceStatus() == 1){ // 状态码 1: 可以接单. 调用远程服务更新司机位置信息
            Result<Boolean> booleanResult = locationFeignClient.updateDriverLocation(updateDriverLocationForm);
            if (booleanResult.getCode() != 200) {
                return false;
            }
            return booleanResult.getData();
        }else{
            throw new GuiguException(ResultCodeEnum.NO_START_SERVICE);
        }
    }

    @Override
    public Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm) {
        Result<Boolean> booleanResult = locationFeignClient.updateOrderLocationToCache(updateOrderLocationForm);
        if (booleanResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }
        return booleanResult.getData();
    }
}
