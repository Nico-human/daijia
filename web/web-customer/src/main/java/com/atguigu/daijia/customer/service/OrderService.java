package com.atguigu.daijia.customer.service;

import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;

public interface OrderService {

    /**
     * 远程调用获取驾驶路线, 并预估订单费用, 返回包含这两个信息的Vo对象
     * @param expectOrderForm
     * @return ExpectOrderVo对象, 其属性值包含(驾驶路线DrivingLineVo, 订单费用feeRuleResponseVo)
     */
    ExpectOrderVo expectOrder(ExpectOrderForm expectOrderForm);

    /**
     * 提交订单:
     * 1. 重新计算驾驶路线
     * 2. 重新计算订单费用
     * 3. 保存订单信息
     * 4. 查询附近可以接单的司机(任务调度实现)
     * @param submitOrderForm
     * @return orderId 订单Id
     */
    Long submitOrder(SubmitOrderForm submitOrderForm);

    /**
     * 获取订单状态(完成, 未完成......)
     * @param orderId
     * @return 订单状态码
     */
    Integer getOrderStatus(Long orderId);

    /**
     * 乘客端查找当前订单
     * @param customerId
     * @return
     */
    CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId);

    /**
     * 乘客端获取订单信息
     * @param orderId
     * @param customerId
     * @return
     */
    OrderInfoVo getOrderInfo(Long orderId, Long customerId);

    /**
     * 司乘同显: 乘客根据订单Id获取司机基本信息
     * @param orderId
     * @param customerId
     * @return
     */
    DriverInfoVo getDriverInfo(Long orderId, Long customerId);

    /**
     * 司乘同显: 乘客获取订单经纬度信息
     * @param orderId
     * @return
     */
    OrderLocationVo getCacheOrderLocation(Long orderId);

    /**
     * 司乘同显: 计算最佳驾驶线路
     * @param calculateDrivingLineForm
     * @return
     */
    DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm);

    /**
     * 代驾服务: 获取订单服务最后一个位置信息
     * @param orderId
     * @return
     */
    OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId);

    /**
     * 获取乘客订单分页列表
     * @param customerId
     * @param page
     * @param limit
     * @return
     */
    PageVo findCustomerOrderPage(Long customerId, Long page, Long limit);

}
