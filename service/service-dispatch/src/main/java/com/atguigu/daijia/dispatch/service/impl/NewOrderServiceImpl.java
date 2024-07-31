package com.atguigu.daijia.dispatch.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.dispatch.mapper.OrderJobMapper;
import com.atguigu.daijia.dispatch.service.NewOrderService;
import com.atguigu.daijia.dispatch.xxl.client.XxlJobClient;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.entity.dispatch.OrderJob;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class NewOrderServiceImpl implements NewOrderService {

    @Autowired
    private OrderJobMapper orderJobMapper;
    @Autowired
    private XxlJobClient xxlJobClient;
    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;
    @Autowired
    private LocationFeignClient locationFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Long addAndStartTask(NewOrderTaskVo newOrderTaskVo) {
        //1. 根据订单id判断是否已经开启任务调度
        LambdaQueryWrapper<OrderJob> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderJob::getOrderId, newOrderTaskVo.getOrderId());
        OrderJob orderJob = orderJobMapper.selectOne(queryWrapper);

        //2. 没有开启，则开启任务调度
        if (orderJob == null) {
            Long jobId = xxlJobClient.addJob("newOrderTaskHandler", //具体JobHandler方法
                                            "",
                                            "0 0/1 * * * ?", // cron表达式
                                            "新创建订单任务调度： " + newOrderTaskVo.getOrderId());

            orderJob = new OrderJob();
            orderJob.setOrderId(newOrderTaskVo.getOrderId());
            orderJob.setJobId(jobId);
            orderJob.setParameter(JSONObject.toJSONString(newOrderTaskVo));
            orderJobMapper.insert(orderJob);
        }
        return orderJob.getJobId();
    }

    @Override
    public void executeTask(long jobId) {

        // 1.查询此任务是否存在
        LambdaQueryWrapper<OrderJob> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderJob::getJobId, jobId);
        OrderJob orderJob = orderJobMapper.selectOne(queryWrapper);
        if (orderJob == null) {
            return; // 数据库没有此任务, 说执行器没有创建此任务
        }

        // 2.查询此任务状态(订单是否是待接单状态)
        String parameter = orderJob.getParameter(); // 第64行代码存入了newOrderTaskVo对象, 其中包含OrderId
        NewOrderTaskVo newOrderTaskVo = JSONObject.parseObject(parameter, NewOrderTaskVo.class);
        Long orderId = newOrderTaskVo.getOrderId();
        Result<Integer> orderStatusResult = orderInfoFeignClient.getOrderStatus(orderId); //远程调用获取订单状态
        if (orderStatusResult.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }
        Integer status = orderStatusResult.getData();
        if (status.intValue() != OrderStatus.WAITING_ACCEPT.getStatus()) {
            xxlJobClient.stopJob(jobId); // 订单不是待接单状态, 停止此任务
            return;
        }

        // 3.搜索附近满足条件的司机(远程调用)
        SearchNearByDriverForm searchNearByDriverForm = new SearchNearByDriverForm();
        searchNearByDriverForm.setLongitude(newOrderTaskVo.getStartPointLongitude());
        searchNearByDriverForm.setLatitude(newOrderTaskVo.getStartPointLatitude());
        searchNearByDriverForm.setMileageDistance(newOrderTaskVo.getExpectDistance());
        Result<List<NearByDriverVo>> nearByDriverList =                         // 远程调用搜索附近满足条件的司机
                locationFeignClient.searchNearByDriver(searchNearByDriverForm);

        if (nearByDriverList.getCode() != 200) {
            throw new GuiguException(ResultCodeEnum.FEIGN_FAIL);
        }

        // 4.遍历司机集合, 为了每个司机创建临时队列, 存储新订单信息
        nearByDriverList.getData().forEach(nearByDriver -> {
            String orderKey = RedisConstant.DRIVER_ORDER_REPEAT_LIST + newOrderTaskVo.getOrderId(); // 根据订单id生成key
            Boolean isMember = redisTemplate.opsForSet().isMember(orderKey, nearByDriver.getDriverId()); //查看司机是否已经被分配给该订单了
            if (Boolean.FALSE.equals(isMember)) {
                // 一个订单对应多个司机
                redisTemplate.opsForSet().add(orderKey, nearByDriver.getDriverId());
                redisTemplate.expire(orderKey, RedisConstant.DRIVER_ORDER_REPEAT_LIST_EXPIRES_TIME, TimeUnit.MINUTES); // 设置过期时间

                // 一个司机也对应多个订单
                NewOrderDataVo newOrderDataVo = new NewOrderDataVo();
                BeanUtils.copyProperties(newOrderTaskVo, newOrderDataVo);
                String driverKey = RedisConstant.DRIVER_ORDER_TEMP_LIST + nearByDriver.getDriverId(); // 根据司机id生成key
                redisTemplate.opsForList().leftPush(driverKey, JSONObject.toJSONString(newOrderDataVo));
                redisTemplate.expire(driverKey, RedisConstant.DRIVER_ORDER_TEMP_LIST_EXPIRES_TIME, TimeUnit.MINUTES); // 设置过期时间
            }

        });
    }

    @Override
    public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
        ArrayList<NewOrderDataVo> list = new ArrayList<>();
        String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
        Long size = redisTemplate.opsForList().size(key);
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                String content = (String) redisTemplate.opsForList().leftPop(key); // 取出第119行存的数据
                NewOrderDataVo newOrderDataVo = JSONObject.parseObject(content, NewOrderDataVo.class);
                list.add(newOrderDataVo);
            }
        }
        return list;
    }

    @Override
    public Boolean clearNewOrderQueueData(Long driverId) {
        String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
        redisTemplate.delete(key);
        return true;
    }
}
