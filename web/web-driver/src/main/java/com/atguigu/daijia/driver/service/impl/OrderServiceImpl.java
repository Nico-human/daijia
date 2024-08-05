package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.constant.SystemConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.LocationUtil;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.service.OrderService;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.map.client.MapFeignClient;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.enums.OrderStatusEnum;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderFeeForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderBillForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.form.rules.ProfitsharingRuleRequestForm;
import com.atguigu.daijia.model.form.rules.RewardRuleRequestForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.atguigu.daijia.model.vo.order.*;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.model.vo.rules.ProfitsharingRuleResponseVo;
import com.atguigu.daijia.model.vo.rules.RewardRuleResponseVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.rules.client.FeeRuleFeignClient;
import com.atguigu.daijia.rules.client.ProfitsharingRuleFeignClient;
import com.atguigu.daijia.rules.client.RewardRuleFeignClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

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
    @Autowired
    private LocationFeignClient locationFeignClient;
    @Autowired
    private FeeRuleFeignClient feeRuleFeignClient;
    @Autowired
    private RewardRuleFeignClient rewardRuleFeignClient;
    @Autowired
    private ProfitsharingRuleFeignClient profitsharingRuleFeignClient;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

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

        // 获取账单和分账信息
        if (orderInfo.getStatus() >= OrderStatusEnum.END_SERVICE.getStatus()) {
            OrderBillVo orderBillVo = orderInfoFeignClient.getOrderBillInfo(orderId).getData();
            OrderProfitsharingVo orderProfitsharingVo = orderInfoFeignClient.getOrderProfitsharing(orderId).getData();
            orderInfoVo.setOrderBillVo(orderBillVo);
            orderInfoVo.setOrderProfitsharingVo(orderProfitsharingVo);
        }
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

    @Override
    public Boolean driverArriveStartLocation(Long orderId, Long driverId) {

        // 订单开始位置判断是否刷单(司机当前位置 距离 订单开始位置 > 特定值, 也就是说司机还没到代驾订单起始点就点击了代驾开始)
        OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderId).getData();
        OrderLocationVo orderLocationVo = locationFeignClient.getCacheOrderLocation(orderId).getData();

        double distance = LocationUtil.getDistance(orderInfo.getStartPointLatitude().doubleValue(),
                                                   orderInfo.getStartPointLongitude().doubleValue(),
                                                   orderLocationVo.getLatitude().doubleValue(),
                                                   orderLocationVo.getLongitude().doubleValue());
        if(distance > SystemConstant.DRIVER_START_LOCATION_DISTION) {
            throw new GuiguException(ResultCodeEnum.DRIVER_START_LOCATION_DISTION_ERROR);
        }

        Result<Boolean> booleanResult = orderInfoFeignClient.driverArriveStartLocation(orderId, driverId);
        if (booleanResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }
        return booleanResult.getData();
    }

    @Override
    public Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm) {
        Result<Boolean> booleanResult = orderInfoFeignClient.updateOrderCart(updateOrderCartForm);
        if (booleanResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }
        return booleanResult.getData();
    }

    @Override
    public Boolean startDrive(StartDriveForm startDriveForm) {
        Result<Boolean> booleanResult = orderInfoFeignClient.startDrive(startDriveForm);
        if (booleanResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }
        return booleanResult.getData();
    }

    @Override
    public Boolean endDrive(OrderFeeForm orderFeeForm) {
        Result<OrderInfo> orderInfoResult = orderInfoFeignClient.getOrderInfo(orderFeeForm.getOrderId());
        if (orderInfoResult.getCode() != 200 || orderInfoResult.getData().getDriverId() != orderFeeForm.getDriverId()){
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        OrderInfo orderInfo = orderInfoResult.getData();

        // 订单结束位置判断是否刷单(司机当前位置 距离 订单结束位置 > 特定值, 也就是说司机还没到代驾订单结束点就点击了代驾完成)
        OrderServiceLastLocationVo orderServiceLastLocationVo =
                locationFeignClient.getOrderServiceLastLocation(orderFeeForm.getOrderId()).getData();

        double distance = LocationUtil.getDistance(orderInfo.getEndPointLatitude().doubleValue(),
                                                   orderInfo.getEndPointLongitude().doubleValue(),
                                                   orderServiceLastLocationVo.getLatitude().doubleValue(),
                                                   orderServiceLastLocationVo.getLongitude().doubleValue());
        if(distance > SystemConstant.DRIVER_END_LOCATION_DISTION) {
            throw new GuiguException(ResultCodeEnum.DRIVER_END_LOCATION_DISTION_ERROR);
        }

        // 计算订单实际里程
        BigDecimal realDistance = locationFeignClient.calculateOrderRealDistance(orderFeeForm.getOrderId()).getData();

        // 计算代驾实际费用
        FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
        feeRuleRequestForm.setDistance(realDistance);
        feeRuleRequestForm.setStartTime(orderInfo.getStartServiceTime());

        Integer waitMinute = Math.abs( (int) ((
                orderInfo.getArriveTime().getTime()-orderInfo.getAcceptTime().getTime()) / (1000 * 60)));
        feeRuleRequestForm.setWaitMinute(waitMinute);

        FeeRuleResponseVo feeRuleResponseVo = feeRuleFeignClient.calculateOrderFee(feeRuleRequestForm).getData();

        BigDecimal totalAmount = feeRuleResponseVo.getTotalAmount()
                                                        .add(orderFeeForm.getTollFee())
                                                        .add(orderFeeForm.getParkingFee())
                                                        .add(orderFeeForm.getOtherFee())
                                                        .add(orderInfo.getFavourFee());
        feeRuleResponseVo.setTotalAmount(totalAmount);

        // 系统奖励
        String startTime = new DateTime(orderInfo.getStartServiceTime()).toString("yyyy-MM-dd") + " 00:00:00";
        String endTime = new DateTime(orderInfo.getStartServiceTime()).toString("yyyy-MM-dd") + " 24:00:00";
        Long orderNum = orderInfoFeignClient.getOrderNumByTime(startTime, endTime).getData();

        RewardRuleRequestForm rewardRuleRequestForm = new RewardRuleRequestForm();
        rewardRuleRequestForm.setOrderNum(orderNum);
        rewardRuleRequestForm.setStartTime(orderInfo.getStartServiceTime());

        RewardRuleResponseVo rewardRuleResponseVo =
                rewardRuleFeignClient.calculateOrderRewardFee(rewardRuleRequestForm).getData();

        // 分账信息
        ProfitsharingRuleRequestForm profitsharingRuleRequestForm = new ProfitsharingRuleRequestForm();
        profitsharingRuleRequestForm.setOrderNum(orderNum);
        profitsharingRuleRequestForm.setOrderAmount(feeRuleResponseVo.getTotalAmount());

        ProfitsharingRuleResponseVo profitsharingRuleResponseVo =
                profitsharingRuleFeignClient.calculateOrderProfitsharingFee(profitsharingRuleRequestForm).getData();

        // 生成最终账单
        UpdateOrderBillForm updateOrderBillForm = new UpdateOrderBillForm();
        updateOrderBillForm.setOrderId(orderFeeForm.getOrderId());
        updateOrderBillForm.setDriverId(orderFeeForm.getDriverId());
        updateOrderBillForm.setTollFee(orderFeeForm.getTollFee());
        updateOrderBillForm.setParkingFee(orderFeeForm.getParkingFee());
        updateOrderBillForm.setOtherFee(orderFeeForm.getOtherFee());
        updateOrderBillForm.setFavourFee(orderInfo.getFavourFee());
        updateOrderBillForm.setRealDistance(realDistance);
        BeanUtils.copyProperties(feeRuleResponseVo, updateOrderBillForm);
        BeanUtils.copyProperties(rewardRuleResponseVo, updateOrderBillForm);
        BeanUtils.copyProperties(profitsharingRuleResponseVo, updateOrderBillForm);
        updateOrderBillForm.setProfitsharingRuleId(profitsharingRuleResponseVo.getProfitsharingRuleId());

        return orderInfoFeignClient.endDrive(updateOrderBillForm).getData();
    }


    @Override
    public PageVo findDriverOrderPage(Long driverId, Long page, Long limit) {
        return orderInfoFeignClient.findDriverOrderPage(driverId, page, limit).getData();
    }

    @Override
    public Boolean sendOrderBillInfo(Long orderId, Long driverId) {
        return orderInfoFeignClient.sendOrderBillInfo(orderId, driverId).getData();
    }

    /**
     * 执行顺序:
     *          1        (4 -> 5)
     *             ↘   ↗          ↘
     *               3             8 -> 继续串行
     *            ↗    ↘         ↗
     *          2       (6 -> 7)
     * @param orderFeeForm
     * @return
     */
    @SneakyThrows
    public Boolean endDriveThread(OrderFeeForm orderFeeForm) {

        // 1. 根据orderId获取订单信息, 判断当前订单是否司机接单
        CompletableFuture<OrderInfo> orderInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            Result<OrderInfo> orderInfoResult = orderInfoFeignClient.getOrderInfo(orderFeeForm.getOrderId());
            if (orderInfoResult.getCode() != 200 || orderInfoResult.getData().getDriverId() != orderFeeForm.getDriverId()) {
                throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
            }
            return orderInfoResult.getData();
        }, threadPoolExecutor);

        // 2. 根据orderId获取司机的终点位置
        CompletableFuture<OrderServiceLastLocationVo> orderServiceLastLocationVoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            return locationFeignClient.getOrderServiceLastLocation(orderFeeForm.getOrderId()).getData();
        }, threadPoolExecutor);

        // 获取1, 2线程执行结果
        CompletableFuture.allOf(orderInfoCompletableFuture, orderServiceLastLocationVoCompletableFuture).join();
        OrderInfo orderInfo = orderInfoCompletableFuture.get();
        OrderServiceLastLocationVo orderServiceLastLocationVo = orderServiceLastLocationVoCompletableFuture.get();

        // 3. 订单结束位置判断是否刷单(司机当前位置 距离 订单结束位置 > 特定值, 也就是说司机还没到代驾订单结束点就点击了代驾完成)
        double distance = LocationUtil.getDistance(orderInfo.getEndPointLatitude().doubleValue(),
                                                   orderInfo.getEndPointLongitude().doubleValue(),
                                                   orderServiceLastLocationVo.getLatitude().doubleValue(),
                                                   orderServiceLastLocationVo.getLongitude().doubleValue());
        if(distance > SystemConstant.DRIVER_END_LOCATION_DISTION) {
            throw new GuiguException(ResultCodeEnum.DRIVER_END_LOCATION_DISTION_ERROR);
        }

        // 4. 计算订单实际里程
        CompletableFuture<BigDecimal> realDistanceCompletableFuture = CompletableFuture.supplyAsync(() -> {
            BigDecimal realDistance = locationFeignClient.calculateOrderRealDistance(
                    orderFeeForm.getOrderId()).getData();
            return realDistance;
        }, threadPoolExecutor);


        // 5. 计算代驾实际费用
        CompletableFuture<FeeRuleResponseVo> feeRuleResponseVoCompletableFuture =
                realDistanceCompletableFuture.thenApplyAsync((realDistance) -> {
                    FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
                    feeRuleRequestForm.setDistance(realDistance);
                    feeRuleRequestForm.setStartTime(orderInfo.getStartServiceTime());
                    Integer waitMinute = Math.abs((int) ((
                            orderInfo.getArriveTime().getTime() - orderInfo.getAcceptTime().getTime()) / (1000 * 60)));
                    feeRuleRequestForm.setWaitMinute(waitMinute);
                    FeeRuleResponseVo feeRuleResponseVo = feeRuleFeignClient.calculateOrderFee(feeRuleRequestForm).getData();
                    BigDecimal totalAmount = feeRuleResponseVo.getTotalAmount()
                            .add(orderFeeForm.getTollFee())
                            .add(orderFeeForm.getParkingFee())
                            .add(orderFeeForm.getOtherFee())
                            .add(orderInfo.getFavourFee());
                    feeRuleResponseVo.setTotalAmount(totalAmount);
                    return feeRuleResponseVo;
                }, threadPoolExecutor);

        // 6. 订单数量
        CompletableFuture<Long> orderNumCompletableFuture = CompletableFuture.supplyAsync(() -> {
            String startTime = new DateTime(orderInfo.getStartServiceTime()).toString("yyyy-MM-dd") + " 00:00:00";
            String endTime = new DateTime(orderInfo.getStartServiceTime()).toString("yyyy-MM-dd") + " 24:00:00";
            Long orderNum = orderInfoFeignClient.getOrderNumByTime(startTime, endTime).getData();
            return orderNum;
        }, threadPoolExecutor);

        // 7. 系统奖励
        CompletableFuture<RewardRuleResponseVo> rewardRuleResponseVoCompletableFuture =
                orderNumCompletableFuture.thenApplyAsync((orderNum) -> {
                    RewardRuleRequestForm rewardRuleRequestForm = new RewardRuleRequestForm();
                    rewardRuleRequestForm.setOrderNum(orderNum);
                    rewardRuleRequestForm.setStartTime(orderInfo.getStartServiceTime());
                    RewardRuleResponseVo rewardRuleResponseVo =
                            rewardRuleFeignClient.calculateOrderRewardFee(rewardRuleRequestForm).getData();
                    return rewardRuleResponseVo;
                }, threadPoolExecutor);

        // 8. 分账信息
        CompletableFuture<ProfitsharingRuleResponseVo> profitsharingRuleResponseVoCompletableFuture =
                feeRuleResponseVoCompletableFuture.thenCombineAsync(orderNumCompletableFuture, (feeRuleResponseVo, orderNum) -> {
                    ProfitsharingRuleRequestForm profitsharingRuleRequestForm = new ProfitsharingRuleRequestForm();
                    profitsharingRuleRequestForm.setOrderNum(orderNum);
                    profitsharingRuleRequestForm.setOrderAmount(feeRuleResponseVo.getTotalAmount());
                    ProfitsharingRuleResponseVo profitsharingRuleResponseVo =
                            profitsharingRuleFeignClient.calculateOrderProfitsharingFee(profitsharingRuleRequestForm).getData();
                    return profitsharingRuleResponseVo;
                });

        CompletableFuture.allOf(
                realDistanceCompletableFuture,
                orderInfoCompletableFuture,
                feeRuleResponseVoCompletableFuture,
                orderNumCompletableFuture,
                rewardRuleResponseVoCompletableFuture,
                profitsharingRuleResponseVoCompletableFuture
        ).join();

        // 获取执行结果
        BigDecimal realDistance = realDistanceCompletableFuture.get();
        FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoCompletableFuture.get();
        RewardRuleResponseVo rewardRuleResponseVo = rewardRuleResponseVoCompletableFuture.get();
        ProfitsharingRuleResponseVo profitsharingRuleResponseVo = profitsharingRuleResponseVoCompletableFuture.get();

        // 生成最终账单
        UpdateOrderBillForm updateOrderBillForm = new UpdateOrderBillForm();
        updateOrderBillForm.setOrderId(orderFeeForm.getOrderId());
        updateOrderBillForm.setDriverId(orderFeeForm.getDriverId());
        updateOrderBillForm.setTollFee(orderFeeForm.getTollFee());
        updateOrderBillForm.setParkingFee(orderFeeForm.getParkingFee());
        updateOrderBillForm.setOtherFee(orderFeeForm.getOtherFee());
        updateOrderBillForm.setFavourFee(orderInfo.getFavourFee());
        updateOrderBillForm.setRealDistance(realDistance);
        BeanUtils.copyProperties(feeRuleResponseVo, updateOrderBillForm);
        BeanUtils.copyProperties(rewardRuleResponseVo, updateOrderBillForm);
        BeanUtils.copyProperties(profitsharingRuleResponseVo, updateOrderBillForm);
        updateOrderBillForm.setProfitsharingRuleId(profitsharingRuleResponseVo.getProfitsharingRuleId());

        return orderInfoFeignClient.endDrive(updateOrderBillForm).getData();
    }
}
