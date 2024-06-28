package com.atguigu.tingshu.user.stragety.impl;

import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.user.mapper.UserPaidAlbumMapper;
import com.atguigu.tingshu.user.stragety.PaymentStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("1001")
public class AlbumPaymentStrategy implements PaymentStrategy {

    @Autowired
    UserPaidAlbumMapper userPaidAlbumMapper;

    @Override
    public void processPayment(Long userId, String orderNo, List<Long> itemIdList) throws Exception {
        //根据用户id，订单号获取用户已购买的专辑
        UserPaidAlbum userPaidAlbum = userPaidAlbumMapper.selectOne(new LambdaQueryWrapper<UserPaidAlbum>()
                .eq(UserPaidAlbum::getUserId, userId)
                .eq(UserPaidAlbum::getOrderNo, orderNo));
        if(userPaidAlbum!=null){
            //已经更新过，直接返回
            return;
        }
        userPaidAlbum = new UserPaidAlbum();
        userPaidAlbum.setUserId(userId);
        userPaidAlbum.setAlbumId(itemIdList.get(0));
        userPaidAlbum.setOrderNo(orderNo);
        userPaidAlbumMapper.insert(userPaidAlbum);
    }
}
