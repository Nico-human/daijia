package com.atguigu.daijia.order.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
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

    /**
     * 乘客端查找当前订单
     * @param customerId
     * @return
     */
    @GetMapping("/order/info/searchCustomerCurrentOrder/{customerId}")
    Result<CurrentOrderInfoVo> searchCustomerCurrentOrder(@PathVariable Long customerId);

    /**
     * 司机端查找当前订单
     * @param driverId
     * @return
     */
    @GetMapping("/order/info/searchDriverCurrentOrder/{driverId}")
    Result<CurrentOrderInfoVo> searchDriverCurrentOrder(@PathVariable Long driverId);

    /**
     * 根据订单id查找订单信息
     * @param orderId
     * @return
     */
    @GetMapping("/order/info/getOrderInfo/{orderId}")
    Result<OrderInfo> getOrderInfo(@PathVariable Long orderId);

    /**
     * 司机到达代驾起点, 更新订单状态, 到达时间
     * @param orderId
     * @param driverId
     * @return
     */
    @GetMapping("/order/info/driverArriveStartLocation/{orderId}/{driverId}")
    Result<Boolean> driverArriveStartLocation(@PathVariable Long orderId, @PathVariable Long driverId);

    /**
     * 司机到达代驾起点, 更新代驾车辆信息
     * @param updateOrderCartForm
     * @return
     */
    @PostMapping("/order/info/updateOrderCart")
    Result<Boolean> updateOrderCart(@RequestBody UpdateOrderCartForm updateOrderCartForm);

    /**
     * 司机开始代驾, 更新订单状态
     * @param startDriveForm
     * @return
     */
    @PostMapping("/order/info/startDrive")
    Result<Boolean> startDrive(@RequestBody StartDriveForm startDriveForm);
}