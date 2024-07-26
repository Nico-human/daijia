package com.atguigu.daijia.customer.service;

import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface CustomerInfoService extends IService<CustomerInfo> {

    /**
     * 微信小程序登录接口, 根据临时票据(code)返回用户id
     * @param code: (腾讯接口返回的)code值
     * @return 用户id (daijia_customer库中customer_info表的id字段)
     */
    Long login(String code);

    /**
     * 根据传递的用户id查询数据库表(customer_info)中的用户信息, 并封装到CustomerLoginVo对象中,
     * 最后返回这个对象
     * @param customerId, 用户id
     * @return CustomerLoginVo对象, 用户信息
     */
    CustomerLoginVo getCustomerInfo(Long customerId);

    /**
     * 根据code值获取微信绑定手机号码, 更新customer_info表中的用户信息(手机号码)
     * @param updateWxPhoneForm 前端发送的对象
     * @return Boolean 更新是否成功
     */
    Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm);
}
