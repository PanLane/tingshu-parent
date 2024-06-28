package com.atguigu.tingshu.order.receiver;

import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.order.service.OrderInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderReceiver {

    @Autowired
    OrderInfoService orderInfoService;

    @KafkaListener(topics = KafkaConstant.QUEUE_ORDER_PAY_SUCCESS)
    public void orderPaySuccess(ConsumerRecord<String,String> consumerRecord){
        String orderNo = consumerRecord.value();
        log.info("成功监听支付成功消息，订单号:{}",orderNo);
        orderInfoService.paySuccess(orderNo);
    }
}
