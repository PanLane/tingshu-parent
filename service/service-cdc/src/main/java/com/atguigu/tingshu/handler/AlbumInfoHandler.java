package com.atguigu.tingshu.handler;

import com.atguigu.tingshu.entity.CdcEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import top.javatool.canal.client.handler.EntryHandler;

import java.util.Arrays;

@Slf4j
@Component
public class AlbumInfoHandler implements EntryHandler<CdcEntity> {

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void insert(CdcEntity cdcEntity) {
        EntryHandler.super.insert(cdcEntity);
    }

    @Override
    public void update(CdcEntity before, CdcEntity after) {
        log.info("监听到数据发送改变........................更新->id：{}",after.getId());
        redisTemplate.delete("album:info:"+ Arrays.asList(after.getId()));
    }

    @Override
    public void delete(CdcEntity cdcEntity) {
        log.info("监听到数据发送改变........................删除->id：{}",cdcEntity.getId());
        redisTemplate.delete("album:info:"+ Arrays.asList(cdcEntity.getId()));
    }
}
