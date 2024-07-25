package com.atguigu.daijia.customer.service;

public interface CustomerService {

    /**
     * 微信小程序登录, 通过(service-client.service-customer-client中的CustomerInfoFeignClient接口) (远程调用开始)
     * 远程调用(service.service-customer中CustomerInfoController接口), 获取用户id. (远程调用结束)
     * (返回到 web.web-customer中的CustomerServiceImpl中) 生成token, 将(token, 用户id) 存入Redis中, 返回token
     * @param code: 前端传入的参数
     * @return token
     */
    String login(String code);
}
