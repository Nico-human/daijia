package com.atguigu.daijia.customer.service;

import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;

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
}
