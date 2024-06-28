package com.atguigu.tingshu.user.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserReceiver {

    @Autowired
    private UserInfoService userInfoService;

    //@KafkaListener(topics = KafkaConstant.QUEUE_USER_PAY_RECORD)
    public void userPayRecord(ConsumerRecord<String,String> record) {
        try{
            String userPaidRecordVoStr = record.value();
            if(userPaidRecordVoStr == null) throw new GuiguException(ResultCodeEnum.DATA_ERROR);
            userInfoService.updateUserPaidRecord(JSON.parseObject(userPaidRecordVoStr, UserPaidRecordVo.class));
            log.info("成功收到更新支付成功后状态消息");
        }catch (Exception e){
            log.error("更新支付成功后状态消息接收失败");
            e.printStackTrace();
        }
    }
}
