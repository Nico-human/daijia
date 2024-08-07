package com.atguigu.daijia.order.handle;

import com.atguigu.daijia.order.service.OrderInfoService;
import jakarta.annotation.PostConstruct;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @Description: 监听延迟队列
 * @Author: dong
 * @Date: 2024/8/6
 */
@Component
public class RedisDelayHandler {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private OrderInfoService orderInfoService;

    @PostConstruct
    public void listener() {

        new Thread(() -> {

            while (true) {
                RBlockingQueue<String> blockingQueue = redissonClient.getBlockingQueue("queue_cancel");
                try {
                    String orderId = blockingQueue.take();
                    if (StringUtils.hasText(orderId)) {
                        orderInfoService.cancelOrder(Long.parseLong(orderId));
                    }

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }).start();

    }

}
