package com.atguigu.daijia.customer.service;

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

    /**
     * 根据请求头中的token (login 时生成的token👆), 查看Redis中是否有对应的数据(用户id), 若有
     * 则远程调用获取用户信息, 最后返回用户信息
     * @param token 用户token
     * @return CustomerLoginVo对象, 用户信息
     */
    CustomerLoginVo getCustomerLoginInfo(String token);
}
