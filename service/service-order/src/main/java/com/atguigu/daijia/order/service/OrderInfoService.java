package com.atguigu.daijia.order.service;

import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OrderInfoService extends IService<OrderInfo> {

    /**
     * 保存订单信息(order_info表), 并记录日志(order_status_log表)
     * @param orderInfoForm 前端传入的实体类
     * @return 订单Id
     */
    Long saveOrderInfo(OrderInfoForm orderInfoForm);

    /**
     * 根据订单id查询订单状态
     * @param orderId
     * @return
     */
    Integer getOrderStatus(Long orderId);

    /**
     * 司机抢单(根据司机id, 订单id)
     * @param driverId
     * @param orderId
     * @return 是否抢单成功
     */
    Boolean robNewOrder(Long driverId, Long orderId);
}
