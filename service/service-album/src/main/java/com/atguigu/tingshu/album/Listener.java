package com.atguigu.tingshu.album;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class Listener {

    @Autowired
    TrackInfoService trackInfoService;
    @Autowired
    RedisTemplate redisTemplate;

    @KafkaListener(topics = KafkaConstant.QUEUE_TRACK_STAT_UPDATE)
    public void updateTrackStat(ConsumerRecord<String, String> consumerRecord) {
        TrackStatMqVo trackStatMqVo = JSON.parseObject(consumerRecord.value(),TrackStatMqVo.class);
        //判断用户是否在一天内已更新过播放量
        Boolean updated = redisTemplate.opsForValue().setIfAbsent(RedisConstant.ALBUM_STAT_ENDTIME+trackStatMqVo.getBusinessNo(), trackStatMqVo, 1, TimeUnit.DAYS);
        ////没有更新过，专辑播放量+1，声音播放量+1
        if(updated) trackInfoService.updateTrackStat(trackStatMqVo);
    }
}
