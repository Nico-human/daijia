package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.vo.order.NewOrderDataVo;

import java.util.List;

public interface OrderService {

    /**
     *
     * @param orderId
     * @return
     */
    Integer getOrderStatus(Long orderId);

    /**
     *
     * @param driverId
     * @return
     */
    List<NewOrderDataVo> findNewOrderQueueData(Long driverId);

    /**
     *
     * @param driverId
     * @param orderId
     * @return
     */
    Boolean robNewOrder(Long driverId, Long orderId);
}
