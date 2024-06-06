package com.atguigu.tingshu.search.listener;

import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.search.service.SearchService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AlbumIndexListener {

    @Autowired
    private SearchService searchService;

    @KafkaListener(topics = KafkaConstant.QUEUE_ALBUM_UPPER)
    public void up(ConsumerRecord<String,String> record){
        Long albumId = Long.parseLong(record.value());
        searchService.upperAlbum(albumId);
    }

    @KafkaListener(topics = KafkaConstant.QUEUE_ALBUM_LOWER)
    public void down(ConsumerRecord<String,String> record){
        Long albumId = Long.parseLong(record.value());
        searchService.lowerAlbum(albumId);
    }
}
