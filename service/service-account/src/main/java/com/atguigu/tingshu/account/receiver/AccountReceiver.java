package com.atguigu.tingshu.account.receiver;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AccountReceiver {

    @Autowired
    private UserAccountService userAccountService;

    @KafkaListener(topics = KafkaConstant.QUEUE_USER_REGISTER)
    public void registerAccount(ConsumerRecord<String,String> record) {
        log.info("------------成功监听注册账户消息------------");
        Long userId = Long.parseLong(record.value());
        userAccountService.registerAccount(userId);
    }

    @KafkaListener(topics = KafkaConstant.QUEUE_ACCOUNT_UNLOCK)
    public void unlockAccount(ConsumerRecord<String,String> record) {
        log.info("------------成功监听解锁账户消息------------");
        String orderNo = record.value();
        userAccountService.unlockAccount(orderNo);
    }

    @KafkaListener(topics = KafkaConstant.QUEUE_ACCOUNT_MINUS)
    public void minusAccount(ConsumerRecord<String,String> record) {
        log.info("------------成功监听扣减账户消息------------");
        String orderNo = record.value();
        userAccountService.minusAccount(orderNo);
    }

    @KafkaListener(topics = KafkaConstant.QUEUE_RECHARGE_PAY_SUCCESS)
    public void rechargePaySuccess(ConsumerRecord<String,String> record) {
        log.info("------------成功监听充值成功消息------------");
        String orderNo = record.value();
        userAccountService.rechargePaySuccess(orderNo);
    }


}
