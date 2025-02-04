package com.atguigu.daijia.driver.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(value = "service-driver")
public interface DriverInfoFeignClient {

    /**
     * 小程序授权登录
     * @param code
     * @return 用户id
     */
    @GetMapping("/driver/info/login/{code}")
    Result<Long> login(@PathVariable String code);

    /**
     * 获取司机信息
     * @param driverId 司机id
     * @return 封装司机信息的DriverLoginVo对象
     */
    @RequestMapping("/driver/info/getDriverLoginInfo/{driverId}")
    Result<DriverLoginVo> getDriverLoginInfo(@PathVariable Long driverId);

    /**
     * 回显司机认证信息
     * @param driverId
     * @return
     */
    @RequestMapping("/driver/info/getDriverAuthInfo/{driverId}")
    Result<DriverAuthInfoVo> getDriverAuthInfo(@PathVariable Long driverId);

    /**
     * 更新司机认证信息
     * @param updateDriverAuthInfoForm
     * @return
     */
    @PostMapping("/driver/info/updateDriverAuthInfo")
    Result<Boolean> updateDriverAuthInfo(@RequestBody UpdateDriverAuthInfoForm updateDriverAuthInfoForm);

    /**
     * 创建司机人脸模型
     * @param driverFaceModelForm
     * @return
     */
    @PostMapping("/driver/info/createDriverFaceModel")
    Result<Boolean> createDriverFaceModel(@RequestBody DriverFaceModelForm driverFaceModelForm);

    /**
     * 获取司机个性化设置信息(状态值: 是否可以接单, 接单里程, 订单里程等)
     * @param driverId
     * @return
     */
    @GetMapping("/driver/info/getDriverSet/{driverId}")
    Result<DriverSet> getDriverSet(@PathVariable Long driverId);

    /**
     * 判断司机当日是否进行过人脸识别
     * @param driverId
     * @return
     */
    @GetMapping("driver/info/isFaceRecognition/{driverId}")
    Result<Boolean> isFaceRecognition(@PathVariable Long driverId);

    /**
     * 验证司机人脸
     * @param driverFaceModelForm
     * @return
     */
    @PostMapping("driver/info/verifyDriverFace")
    Result<Boolean> verifyDriverFace(@RequestBody DriverFaceModelForm driverFaceModelForm);

    /**
     * 更新司机接单状态
     * @param driverId
     * @param status
     * @return
     */
    @PostMapping("/driver/info/updateServiceStatus/{driverId}/{status}")
    Result<Boolean> updateServiceStatus(@PathVariable Long driverId, @PathVariable Integer status);

    /**
     * 司乘同显: 获取司机基本信息
     * @param driverId
     * @return
     */
    @GetMapping("/driver/info/getDriverInfo/{driverId}")
    Result<DriverInfoVo> getDriverInfoOrder(@PathVariable Long driverId);

    /**
     * 获取司机OpenId
     * @param driverId
     * @return
     */
    @GetMapping("/driver/info/getDriverOpenId/{driverId}")
    Result<String> getDriverOpenId(@PathVariable Long driverId);
}