package com.atguigu.tingshu.order.handler;

import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.order.service.OrderInfoService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DelayMessageHandler {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private OrderInfoService orderInfoService;

    @PostConstruct
    public void cancelOrder() {
        new Thread(()->{
            RBlockingQueue<Long> blockingQueue = redissonClient.getBlockingQueue(KafkaConstant.QUEUE_ORDER_CANCEL);
            while (true){
                try {
                    Long orderId = blockingQueue.take();
                    orderInfoService.cancelOrder(orderId);
                    log.info("接收延迟消息成功,订单id：{}",orderId);
                } catch (InterruptedException e) {
                    log.info("接收延迟消息失败");
                    e.printStackTrace();
                }
            }
        }).start();

    }
}
