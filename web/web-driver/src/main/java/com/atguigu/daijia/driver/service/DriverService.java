package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;

public interface DriverService {

    /**
     * 根据前端传递的code值进行远程调用, 并生成token
     * 将 (token, 远程调用返回的用户id) 存入redis中, 设置过期时间
     * 最后返回token
     * @param code
     * @return
     */
    String login(String code);

    /**
     * 根据司机id远程调用获取司机登录信息(封装对象), 并返回封装对象
     * @param driverId 司机id
     * @return DriverLoginVo封装对象
     */
    DriverLoginVo getDriverLoginInfo(Long driverId);

    /**
     * 根据司机id远程调用获取司机认证信息, 并返回封装对象
     * @param driverId 司机id
     * @return Vo对象
     */
    DriverAuthInfoVo getDriverAuthInfo(Long driverId);

    /**
     * 更新司机认证信息
     * @param updateDriverAuthInfoForm
     * @return
     */
    Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm);

    /**
     * 创建司机人脸模型
     * @param driverFaceModelForm
     * @return
     */
    Boolean createDriverFaceModel(DriverFaceModelForm driverFaceModelForm);
}
