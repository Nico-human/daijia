package com.atguigu.daijia.customer.service;

import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface CustomerInfoService extends IService<CustomerInfo> {

    /**
     * 微信小程序登录接口, 根据临时票据(code)返回用户id
     * @param code: (腾讯接口返回的)code值
     * @return 用户id (daijia_customer库中customer_info表的id字段)
     */
    Long login(String code);

}
