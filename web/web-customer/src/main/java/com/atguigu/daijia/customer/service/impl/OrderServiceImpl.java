package com.atguigu.daijia.customer.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.customer.service.OrderService;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.map.client.MapFeignClient;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.rules.client.FeeRuleFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Date;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderServiceImpl implements OrderService {

    @Autowired
    private MapFeignClient mapFeignClient;
    @Autowired
    private FeeRuleFeignClient feeRuleFeignClient;
    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;
    @Autowired
    private NewOrderFeignClient newOrderFeignClient;
    @Autowired
    private DriverInfoFeignClient driverInfoFeignClient;
    @Autowired
    private LocationFeignClient locationFeignClient;

    @Override
    public ExpectOrderVo expectOrder(ExpectOrderForm expectOrderForm) {

        //1.获取驾驶路线
        CalculateDrivingLineForm calculateDrivingLineForm = new CalculateDrivingLineForm();
        BeanUtils.copyProperties(expectOrderForm, calculateDrivingLineForm);
        Result<DrivingLineVo> drivingLineVoResult = mapFeignClient.calculateDrivingLine(calculateDrivingLineForm);

        if (drivingLineVoResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.MAP_FAIL);
        }
        DrivingLineVo drivingLineVo = drivingLineVoResult.getData();

        //2.获取订单费用
        FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
        feeRuleRequestForm.setDistance(drivingLineVo.getDistance());
        feeRuleRequestForm.setStartTime(new Date());
        feeRuleRequestForm.setWaitMinute(0);
        Result<FeeRuleResponseVo> feeRuleResponseVoResult = feeRuleFeignClient.calculateOrderFee(feeRuleRequestForm);
        if (feeRuleResponseVoResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }
        FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoResult.getData();

        //3.封装Vo对象
        ExpectOrderVo expectOrderVo = new ExpectOrderVo();
        expectOrderVo.setDrivingLineVo(drivingLineVo);
        expectOrderVo.setFeeRuleResponseVo(feeRuleResponseVo);

        return expectOrderVo;
    }

    @Override
    public Long submitOrder(SubmitOrderForm submitOrderForm) {

        // 1.重新计算驾驶路线
        CalculateDrivingLineForm calculateDrivingLineForm = new CalculateDrivingLineForm();
        BeanUtils.copyProperties(submitOrderForm, calculateDrivingLineForm);
        Result<DrivingLineVo> drivingLineVoResult = mapFeignClient.calculateDrivingLine(calculateDrivingLineForm);
        if (drivingLineVoResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.MAP_FAIL);
        }
        DrivingLineVo drivingLineVo = drivingLineVoResult.getData();

        // 2.重新计算订单费用
        FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
        feeRuleRequestForm.setDistance(drivingLineVo.getDistance());
        feeRuleRequestForm.setStartTime(new Date());
        feeRuleRequestForm.setWaitMinute(0);
        Result<FeeRuleResponseVo> feeRuleResponseVoResult = feeRuleFeignClient.calculateOrderFee(feeRuleRequestForm);
        if (feeRuleResponseVoResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }
        FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoResult.getData();

        // 3.保存订单信息
        OrderInfoForm orderInfoForm = new OrderInfoForm();
        BeanUtils.copyProperties(submitOrderForm, orderInfoForm);
        orderInfoForm.setExpectDistance(drivingLineVo.getDistance());
        orderInfoForm.setExpectAmount(feeRuleResponseVo.getTotalAmount());
        Result<Long> orderIdResult = orderInfoFeignClient.saveOrderInfo(orderInfoForm);
        if (orderIdResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.SERVICE_ERROR);
        }
        Long orderId = orderIdResult.getData();

        // 4. (任务调度实现)查询附近可以接单的司机
        NewOrderTaskVo newOrderTaskVo = new NewOrderTaskVo();
        BeanUtils.copyProperties(orderInfoForm, newOrderTaskVo);
        newOrderTaskVo.setOrderId(orderId);
        newOrderTaskVo.setExpectTime(drivingLineVo.getDuration());
        newOrderTaskVo.setExpectDistance(drivingLineVo.getDistance());
        newOrderTaskVo.setCreateTime(new Date());
        Result<Long> jobIdResult = newOrderFeignClient.addAndStartTask(newOrderTaskVo);
        if (jobIdResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.SERVICE_ERROR);
        }
        Long jobId = jobIdResult.getData();

        return orderId;
    }

    @Override
    public Integer getOrderStatus(Long orderId) {
        Result<Integer> orderStatusResult = orderInfoFeignClient.getOrderStatus(orderId);
        if (orderStatusResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.SERVICE_ERROR);
        }
        return orderStatusResult.getData();
    }

    @Override
    public CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId) {
        Result<CurrentOrderInfoVo> currentOrderInfoVoResult = orderInfoFeignClient.searchCustomerCurrentOrder(customerId);
        if (currentOrderInfoVoResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }
        return currentOrderInfoVoResult.getData();
    }

    @Override
    public OrderInfoVo getOrderInfo(Long orderId, Long customerId) {
        Result<OrderInfo> orderInfoResult = orderInfoFeignClient.getOrderInfo(orderId);
        if (orderInfoResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }
        OrderInfo orderInfo = orderInfoResult.getData();

        if (!customerId.equals(orderInfo.getCustomerId())) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }

        OrderInfoVo orderInfoVo = new OrderInfoVo();
        orderInfoVo.setOrderId(orderId);
        BeanUtils.copyProperties(orderInfo, orderInfoVo);
        return orderInfoVo;
    }

    @Override
    public DriverInfoVo getDriverInfo(Long orderId, Long customerId) {
        //根据订单Id获取订单信息
        Result<OrderInfo> orderInfoResult = orderInfoFeignClient.getOrderInfo(orderId);
        if (orderInfoResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }
        OrderInfo orderInfo = orderInfoResult.getData();

        if (!customerId.equals(orderInfo.getCustomerId())) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }

        Result<DriverInfoVo> driverInfoVoResult = driverInfoFeignClient.getDriverInfoOrder(orderInfo.getDriverId());
        if (driverInfoVoResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }
        return driverInfoVoResult.getData();
    }

    @Override
    public OrderLocationVo getCacheOrderLocation(Long orderId) {
        Result<OrderLocationVo> cacheOrderLocationResult = locationFeignClient.getCacheOrderLocation(orderId);
        if (cacheOrderLocationResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }
        return cacheOrderLocationResult.getData();
    }

    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        Result<DrivingLineVo> drivingLineVoResult = mapFeignClient.calculateDrivingLine(calculateDrivingLineForm);
        if (drivingLineVoResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }
        return drivingLineVoResult.getData();
    }
}
