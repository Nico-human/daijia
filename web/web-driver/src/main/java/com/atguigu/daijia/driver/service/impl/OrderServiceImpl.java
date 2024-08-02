package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.service.OrderService;
import com.atguigu.daijia.map.client.MapFeignClient;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;
    @Autowired
    private NewOrderFeignClient newOrderFeignClient;
    @Autowired
    private MapFeignClient mapFeignClient;

    @Override
    public Integer getOrderStatus(Long orderId) {
        Result<Integer> orderStatusResult = orderInfoFeignClient.getOrderStatus(orderId);
        if (orderStatusResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        return orderStatusResult.getData();
    }

    @Override
    public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
        Result<List<NewOrderDataVo>> listResult = newOrderFeignClient.findNewOrderQueueData(driverId);
        if (listResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        return listResult.getData();
    }

    @Override
    public Boolean robNewOrder(Long driverId, Long orderId) {
        Result<Boolean> result = orderInfoFeignClient.robNewOrder(driverId, orderId);
        if (result.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }
        return result.getData();
    }

    @Override
    public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
        Result<CurrentOrderInfoVo> result = orderInfoFeignClient.searchDriverCurrentOrder(driverId);
        if (result.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }
        return result.getData();
    }

    @Override
    public OrderInfoVo getOrderInfo(Long orderId, Long driverId) {
        Result<OrderInfo> orderInfoResult = orderInfoFeignClient.getOrderInfo(orderId);
        if (orderInfoResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }
        OrderInfo orderInfo = orderInfoResult.getData();

        if (!driverId.equals(orderInfo.getDriverId())) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }

        OrderInfoVo orderInfoVo = new OrderInfoVo();
        orderInfoVo.setOrderId(orderId);
        BeanUtils.copyProperties(orderInfo, orderInfoVo);

        return orderInfoVo;
    }

    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        Result<DrivingLineVo> drivingLineVoResult =
                mapFeignClient.calculateDrivingLine(calculateDrivingLineForm);
        if (drivingLineVoResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }
        return drivingLineVoResult.getData();
    }

}
