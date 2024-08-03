package com.atguigu.daijia.order.service;

import com.atguigu.daijia.model.entity.order.OrderMonitor;
import com.atguigu.daijia.model.entity.order.OrderMonitorRecord;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OrderMonitorService extends IService<OrderMonitor> {

    /**
     * 保存订单监控记录数据到MongoDB中
     * @param orderMonitorRecord
     * @return
     */
    Boolean saveOrderMonitorRecord(OrderMonitorRecord orderMonitorRecord);
}
