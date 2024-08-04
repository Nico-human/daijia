package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderFeeForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;

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

    /**
     *
     * @param driverId
     * @return
     */
    CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId);

    /**
     *
     * @param orderId
     * @param driverId
     * @return
     */
    OrderInfoVo getOrderInfo(Long orderId, Long driverId);

    /**
     *
     * @param calculateDrivingLineForm
     * @return
     */
    DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm);

    /**
     *
     * @param orderId
     * @param driverId
     * @return
     */
    Boolean driverArriveStartLocation(Long orderId, Long driverId);

    /**
     *
     * @param updateOrderCartForm
     * @return
     */
    Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm);

    /**
     * 司机开始代驾, 更新订单状态
     * @param startDriveForm
     * @return
     */
    Boolean startDrive(StartDriveForm startDriveForm);

    /**
     *
     * @param orderFeeForm
     * @return
     */
    Boolean endDrive(OrderFeeForm orderFeeForm);

    /**
     *
     * @param driverId
     * @param page
     * @param limit
     * @return
     */
    PageVo findDriverOrderPage(Long driverId, Long page, Long limit);
}
