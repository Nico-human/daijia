package com.atguigu.daijia.dispatch.xxl.job;

import com.atguigu.daijia.dispatch.mapper.XxlJobLogMapper;
import com.atguigu.daijia.dispatch.service.NewOrderService;
import com.atguigu.daijia.model.entity.dispatch.XxlJobLog;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Description: job方法
 * @Author: dong
 * @Date: 2024/7/30
 */
@Component
public class JobHandler {

    @Autowired
    private NewOrderService newOrderService;

    @Autowired
    private XxlJobLogMapper xxlJobLogMapper;

    @XxlJob("newOrderTaskHandler") // 在NewOrderServiceImpl第56行中被加入调度中心
    public void newOrderTaskHandler() { //具体Job任务, 由调度中心调度,

        XxlJobLog xxlJobLog = new XxlJobLog();
        xxlJobLog.setJobId(XxlJobHelper.getJobId());
        long startTime = System.currentTimeMillis();

        try {
            //执行任务
            newOrderService.executeTask(XxlJobHelper.getJobId());
            xxlJobLog.setStatus(1); // 设置任务状态 1: 已完成
        } catch (Exception e) {
            xxlJobLog.setStatus(0);
            xxlJobLog.setError(e.getMessage());
            e.printStackTrace();
        } finally {
            long times = System.currentTimeMillis() - startTime;
            xxlJobLog.setTimes(times);
            xxlJobLogMapper.insert(xxlJobLog);
        }

    }

}
