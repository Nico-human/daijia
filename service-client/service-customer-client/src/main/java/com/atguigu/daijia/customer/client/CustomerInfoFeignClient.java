package com.atguigu.daijia.customer.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "service-customer")
//@RequestMapping("/customer/info") Caused by: java.lang.IllegalArgumentException: @RequestMapping annotation not allowed on @FeignClient interfaces
public interface CustomerInfoFeignClient {

    @GetMapping("/customer/info/login/{code}")
    Result<Long> login(@PathVariable String code);

    @GetMapping("/customer/info/getCustomerInfo/{customerId}")
    Result<CustomerLoginVo> getCustomerInfo(@PathVariable Long customerId);
}