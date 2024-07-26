package com.atguigu.daijia.driver.service;

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
     * 根据司机id远程调用获取司机信息(封装对象), 并返回封装对象
     * @param driverId 司机id
     * @return DriverLoginVo封装对象
     */
    DriverLoginVo getDriverLoginInfo(Long driverId);
}
