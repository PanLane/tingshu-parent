package com.atguigu.tingshu.user.stragety.impl;

import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.mapper.UserPaidAlbumMapper;
import com.atguigu.tingshu.user.mapper.UserPaidTrackMapper;
import com.atguigu.tingshu.user.service.UserPaidTrackService;
import com.atguigu.tingshu.user.stragety.PaymentStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;

@Service("1002")
public class TrackPaymentStrategy implements PaymentStrategy {

    @Autowired
    UserPaidTrackMapper userPaidTrackMapper;
    @Autowired
    TrackInfoFeignClient trackInfoFeignClient;
    @Autowired
    UserPaidTrackService userPaidTrackService;

    @Override
    public void processPayment(Long userId, String orderNo, List<Long> itemIdList) throws Exception {
        //根据用户id，订单号获取用户已购买的声音数量
        int count = userPaidTrackMapper.selectPaidTrackCount();
        if(count>0){
            //已经更新过，直接返回
            return;
        }
        //调用声音微服务客户端，根据声音id获取专辑id
        Result<Long> albumIdResult= trackInfoFeignClient.getAlbumIdByTrackId(itemIdList.get(0));
        Assert.notNull(albumIdResult,"专辑id结果集为空");
        Long albumId = albumIdResult.getData();
        Assert.notNull(albumId,"专辑id为空");

        List<UserPaidTrack> userPaidTrackList = itemIdList.stream().map(itemId -> {
            UserPaidTrack userPaidTrack = new UserPaidTrack();
            userPaidTrack.setUserId(userId);
            userPaidTrack.setOrderNo(orderNo);
            userPaidTrack.setAlbumId(albumId);
            userPaidTrack.setTrackId(itemId);
            return userPaidTrack;
        }).collect(Collectors.toList());
        //批量保存
        userPaidTrackService.saveBatch(userPaidTrackList);
    }
}
