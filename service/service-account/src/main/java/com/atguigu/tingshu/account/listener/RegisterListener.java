package com.atguigu.tingshu.account.listener;

import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.model.account.UserAccount;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RegisterListener {

    @Autowired
    private UserAccountMapper userAccountMapper;

    @KafkaListener(topics = KafkaConstant.QUEUE_USER_REGISTER)
    public void registerAccount(ConsumerRecord<String,String> record) {
        Long userId = Long.parseLong(record.value());
        UserAccount userAccount = new UserAccount();
        userAccount.setUserId(userId);
        userAccount.setTotalAmount(new BigDecimal(1000));
        userAccount.setAvailableAmount(new BigDecimal(1000));
        userAccount.setTotalIncomeAmount(new BigDecimal(1000));
        userAccountMapper.insert(userAccount);
    }
}
