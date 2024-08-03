package com.atguigu.daijia.order.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.entity.order.OrderStatusLog;
import com.atguigu.daijia.model.enums.OrderStatusEnum;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.order.mapper.OrderInfoMapper;
import com.atguigu.daijia.order.mapper.OrderStatusLogMapper;
import com.atguigu.daijia.order.service.OrderInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RLock;

import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderStatusLogMapper orderStatusLogMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Override
    public Long saveOrderInfo(OrderInfoForm orderInfoForm) {

        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(orderInfoForm, orderInfo);

        //生成订单号, 状态, 创建时间等信息
        String OrderNo = UUID.randomUUID().toString().replaceAll("-", "");
        orderInfo.setOrderNo(OrderNo);
        orderInfo.setStatus(OrderStatusEnum.WAITING_ACCEPT.getStatus());
        orderInfo.setCreateTime(new Date());
        orderInfoMapper.insert(orderInfo);

        //记录日志
        OrderStatusLog orderStatusLog = new OrderStatusLog();
        orderStatusLog.setOrderId(orderInfo.getId());
        orderStatusLog.setOrderStatus(orderInfo.getStatus());
        orderStatusLog.setOperateTime(new Date());
        orderStatusLogMapper.insert(orderStatusLog);

        // 向redis中添加标识
        redisTemplate.opsForValue().set(RedisConstant.ORDER_ACCEPT_MARK,
                                        "0",
                                        RedisConstant.ORDER_ACCEPT_MARK_EXPIRES_TIME,
                                        TimeUnit.MINUTES);

        return orderInfo.getId();
    }

    @Override
    public Integer getOrderStatus(Long orderId) {

        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, orderId);
        queryWrapper.select(OrderInfo::getStatus);
        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);

        if (orderInfo == null) {
            return OrderStatusEnum.NULL_ORDER.getStatus();
        }

        return orderInfo.getStatus();
    }

    @Override
    public Boolean robNewOrder(Long driverId, Long orderId) {

        if (Boolean.FALSE.equals(redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK))){
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        }

        RLock lock = redissonClient.getLock(RedisConstant.ROB_NEW_ORDER_LOCK + orderId); // Redisson分布式锁, 锁名称必须唯一
        try {
            boolean isLock = lock.tryLock(RedisConstant.ROB_NEW_ORDER_LOCK_WAIT_TIME,
                                          RedisConstant.ROB_NEW_ORDER_LOCK_LEASE_TIME, TimeUnit.SECONDS);

            if (isLock && Boolean.TRUE.equals(redisTemplate.hasKey(RedisConstant.ORDER_ACCEPT_MARK))) {
                LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(OrderInfo::getId, orderId);
                wrapper.eq(OrderInfo::getStatus, OrderStatusEnum.WAITING_ACCEPT.getStatus());  // 版本号 实现乐观锁;

                OrderInfo orderInfo = new OrderInfo();
                orderInfo.setStatus(OrderStatusEnum.ACCEPTED.getStatus());
                orderInfo.setDriverId(driverId);
                orderInfo.setAcceptTime(new Date());
                int updated = orderInfoMapper.update(orderInfo, wrapper);

                // 抢单失败
                if (updated != 1) {
                    throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
                }
                // 删除redis中的标识
                redisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK);
            }

        } catch (InterruptedException e) {
            throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
        } finally {
            if (lock.isLocked()) {
                lock.unlock();
            }
        }
        return true;
    }

    @Override
    public CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId) {

        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getCustomerId, customerId);

        Integer[] statusArray = {
                OrderStatusEnum.ACCEPTED.getStatus(),
                OrderStatusEnum.DRIVER_ARRIVED.getStatus(),
                OrderStatusEnum.UPDATE_CART_INFO.getStatus(),
                OrderStatusEnum.START_SERVICE.getStatus(),
                OrderStatusEnum.END_SERVICE.getStatus(),
                OrderStatusEnum.UNPAID.getStatus()
        };
        queryWrapper.in(OrderInfo::getStatus, statusArray);
        queryWrapper.orderByDesc(OrderInfo::getId);
        queryWrapper.last("limit 1");
        // select * from order_info where customer_id = ? and status in (?, ?, ?...) order by id DESC limit 1;
        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);

        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        if (orderInfo != null) {
            currentOrderInfoVo.setOrderId(orderInfo.getId());
            currentOrderInfoVo.setStatus(orderInfo.getStatus());
            currentOrderInfoVo.setIsHasCurrentOrder(true);
        } else {
            currentOrderInfoVo.setIsHasCurrentOrder(false);
        }
        return currentOrderInfoVo;
    }

    @Override
    public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {

        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getDriverId, driverId);

        Integer[] statusArray = {
                OrderStatusEnum.ACCEPTED.getStatus(),
                OrderStatusEnum.DRIVER_ARRIVED.getStatus(),
                OrderStatusEnum.UPDATE_CART_INFO.getStatus(),
                OrderStatusEnum.START_SERVICE.getStatus(),
                OrderStatusEnum.END_SERVICE.getStatus()
        };
        queryWrapper.in(OrderInfo::getStatus, statusArray);
        queryWrapper.orderByDesc(OrderInfo::getId);
        queryWrapper.last("limit 1");
        // select * from order_info where driver_id = ? and status in (?, ?, ?...) order by id DESC limit 1;
        OrderInfo orderInfo = orderInfoMapper.selectOne(queryWrapper);

        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        if (orderInfo != null) {
            currentOrderInfoVo.setOrderId(orderInfo.getId());
            currentOrderInfoVo.setStatus(orderInfo.getStatus());
            currentOrderInfoVo.setIsHasCurrentOrder(true);
        } else {
            currentOrderInfoVo.setIsHasCurrentOrder(false);
        }
        return currentOrderInfoVo;
    }

    @Override
    public Boolean driverArriveStartLocation(Long orderId, Long driverId) {
        // 条件: orderId + driverId
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.eq(OrderInfo::getDriverId, driverId);
        // 更新订单状态和到达时间
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatusEnum.DRIVER_ARRIVED.getStatus());
        orderInfo.setArriveTime(new Date());

        int updated = orderInfoMapper.update(orderInfo, wrapper);
        if (updated != 1) {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        } else {
            return true;
        }

    }

    @Override
    public Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm) {

        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, updateOrderCartForm.getOrderId());
        wrapper.eq(OrderInfo::getDriverId, updateOrderCartForm.getDriverId());

        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(updateOrderCartForm, orderInfo);
        orderInfo.setStatus(OrderStatusEnum.UPDATE_CART_INFO.getStatus());

        int rows = orderInfoMapper.update(orderInfo, wrapper);
        if (rows != 1) {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        } else {
            return true;
        }
    }

    @Override
    public Boolean startDriver(StartDriveForm startDriveForm) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatusEnum.START_SERVICE.getStatus());
        orderInfo.setStartServiceTime(new Date());
        //根据订单id, 司机id更新订单状态
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, startDriveForm.getOrderId());
        wrapper.eq(OrderInfo::getDriverId, startDriveForm.getDriverId());

        int rows = orderInfoMapper.update(orderInfo, wrapper);
        if (rows != 1) {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        } else {
            return true;
        }
    }

//    public void log(Long orderId, Integer status){
//        OrderStatusLog orderStatusLog = new OrderStatusLog();
//        orderStatusLog.setOrderId(orderId);
//        orderStatusLog.setOrderStatus(status);
//        orderStatusLog.setOperateTime(new Date());
//        orderStatusLogMapper.insert(orderStatusLog);
//    }


}
