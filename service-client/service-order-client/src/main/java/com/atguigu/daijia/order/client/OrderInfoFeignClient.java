package com.atguigu.daijia.order.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


@FeignClient(value = "service-order")
public interface OrderInfoFeignClient {

    /**
     * 保存订单信息(order_info表), 并记录日志(order_status_log表)
     * @param orderInfoForm 前端传入的实体类
     * @return 订单Id的Result封装对象
     */
    @PutMapping("order/info/saveOrderInfo")
    Result<Long> saveOrderInfo(@RequestBody OrderInfoForm orderInfoForm);

    /**
     * 根据订单id获取订单状态
     * @param orderId
     * @return
     */
    @GetMapping("/order/info/getOrderStatus/{orderId}")
    Result<Integer> getOrderStatus(@PathVariable Long orderId);

    /**
     * 司机抢单(通过redisson分布式锁实现分布式线程安全)
     * @param driverId
     * @param orderId
     * @return
     */
    @GetMapping("/order/info/robNewOrder/{driverId}/{orderId}")
    Result<Boolean> robNewOrder(@PathVariable Long driverId, @PathVariable Long orderId);

}