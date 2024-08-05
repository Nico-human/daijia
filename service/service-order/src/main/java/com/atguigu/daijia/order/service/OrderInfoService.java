package com.atguigu.daijia.order.service;

import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderBillForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderBillVo;
import com.atguigu.daijia.model.vo.order.OrderPayVo;
import com.atguigu.daijia.model.vo.order.OrderProfitsharingVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    /**
     * 乘客端查找当前订单
     * @param customerId
     * @return
     */
    CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId);

    /**
     * 司机端查找当前订单
     * @param driverId
     * @return
     */
    CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId);

    /**
     * 司机到达代驾起点, 更新订单状态和到达时间
     * @param orderId
     * @param driverId
     * @return
     */
    Boolean driverArriveStartLocation(Long orderId, Long driverId);

    /**
     * 司机到达代驾起点, 更新代驾车辆信息
     * @param updateOrderCartForm
     * @return
     */
    Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm);

    /**
     * 司机开始代驾, 更新订单状态
     * @param startDriveForm
     * @return
     */
    Boolean startDriver(StartDriveForm startDriveForm);

    /**
     * 根据时间段获取订单数
     * @param startTime
     * @param endTime
     * @return
     */
    Long getOrderNumByTime(String startTime, String endTime);

    /**
     * 结束代驾更新账单
     * @param updateOrderBillForm
     * @return
     */
    Boolean endDrive(UpdateOrderBillForm updateOrderBillForm);

    /**
     * 乘客我的订单分页查询
     * @param pageParam
     * @param customerId
     * @return
     */
    PageVo findCustomerOrderPage(Page<OrderInfo> pageParam, Long customerId);

    /**
     * 司机我的订单分页查询
     * @param pageParam
     * @param driverId
     * @return
     */
    PageVo findDriverOrderPage(Page<OrderInfo> pageParam, Long driverId);

    /**
     * 根据订单id获取实际订单信息
     * @param orderId
     * @return
     */
    OrderBillVo getOrderBillInfo(Long orderId);

    /**
     * 根据订单id获取实际分账信息
     * @param orderId
     * @return
     */
    OrderProfitsharingVo getOrderProfitsharing(Long orderId);

    /**
     * 发送账单信息
     * @param orderId
     * @param driverId
     * @return
     */
    Boolean sendOrderBillInfo(Long orderId, Long driverId);

    /**
     * 获取订单支付信息
     * @param orderNo
     * @param customerId
     * @return
     */
    OrderPayVo getOrderPayVo(String orderNo, Long customerId);
}
