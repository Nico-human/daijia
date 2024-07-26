package com.atguigu.daijia.customer.service;

import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;

public interface CustomerService {

    /**
     * 微信小程序登录, 通过(service-client.service-customer-client中的CustomerInfoFeignClient接口) (远程调用开始)
     * 远程调用(service.service-customer中CustomerInfoController接口), 获取用户id. (远程调用结束)
     * (返回到 web.web-customer中的CustomerServiceImpl中) 生成token, 将(token, 用户id) 存入Redis中, 最后返回token
     * @param code: 前端传入的参数
     * @return token
     */
    String login(String code);

//    /**
//     * 根据请求头中的token (login 时生成的token👆), 查看Redis中是否有对应的数据(用户id), 若有
//     * 则远程调用获取用户信息, 最后返回用户信息
//     * @param token 用户token
//     * @return CustomerLoginVo对象, 用户信息
//     */
//    CustomerLoginVo getCustomerLoginInfo(String token);

    /**
     * 根据自定义注解LoginAuth校验登录后返回的用户id远程调用获取用户信息, 最后返回用户信息
     * @param customerId 用户id
     * @return CustomerLoginVo 用户信息
     */
    CustomerLoginVo getCustomerInfo(Long customerId);

    /**
     * 更新用户信息(手机号码)
     * @param updateWxPhoneForm 对象, 包含code值和用户id
     * @return 是否更新成功
     */
    Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm);
}
