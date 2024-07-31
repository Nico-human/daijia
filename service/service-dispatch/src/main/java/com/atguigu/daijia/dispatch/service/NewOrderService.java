package com.atguigu.daijia.dispatch.service;

import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;

import java.util.List;

public interface NewOrderService {

    /**
     * 判断是否开启任务调度,
     * 若没有, 则开启任务调度(调用xxlJobClient.addJob方法开启), 具体job方法为JobHandler.newOrderTaskHandler
     * 返回任务id
     * @param newOrderTaskVo
     * @return 调度任务的id
     */
    Long addAndStartTask(NewOrderTaskVo newOrderTaskVo);

    /**
     * 执行调度任务
     * @param jobId
     */
    void executeTask(long jobId);

    List<NewOrderDataVo> findNewOrderQueueData(Long driverId);

    Boolean clearNewOrderQueueData(Long driverId);
}
