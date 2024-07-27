package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.entity.driver.DriverInfo;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface DriverInfoService extends IService<DriverInfo> {

    /**
     * 根据code + 小程序id + 小程序密钥请求腾讯接口, 获取用户openid,
     * 根据openid判断司机是否是第一次登录, 若是, 则添加司机基本信息(driver_info), 相关设置(driver_set), 账户信息(driver_account).
     * 添加司机登录记录(driver_login_log), 最后返回司机的用户id
     * @param code 远程调用发送的code值
     * @return 用户(司机) id
     */
    Long login(String code);

    /**
     * 根据用户(司机)id查询数据库(driver_info表), 将数据封装为DriverLoginVo对象
     * 判断是否需要建档腾讯云人脸模型
     * 最后返回Vo对象
     * @param driverId 司机id
     * @return DriverLoginVo封装对象
     */
    DriverLoginVo getDriverInfo(Long driverId);

    /**
     * 根据司机id查询数据库, 并通过司机信息(身份证, 驾驶证)在腾讯云中的Url,
     * 请求腾讯云接口生成临时Url地址(展示给用户), 将信息封装为Vo对象
     * @param driverId
     * @return
     */
    DriverAuthInfoVo getDriverAuthInfo(Long driverId);

    /**
     *
     * @param updateDriverAuthInfoForm
     * @return
     */
    Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm);

    /**
     *
     * @param driverFaceModelForm
     * @return
     */
    Boolean createDriverFaceModel(DriverFaceModelForm driverFaceModelForm);
}
