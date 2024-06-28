package com.atguigu.tingshu.user.stragety.impl;

import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.model.user.UserVipService;
import com.atguigu.tingshu.model.user.VipServiceConfig;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.mapper.UserPaidAlbumMapper;
import com.atguigu.tingshu.user.mapper.UserVipServiceMapper;
import com.atguigu.tingshu.user.mapper.VipServiceConfigMapper;
import com.atguigu.tingshu.user.stragety.PaymentStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service("1003")
public class VipPaymentStrategy implements PaymentStrategy {

    @Autowired
    UserVipServiceMapper userVipServiceMapper;
    @Autowired
    UserInfoMapper userInfoMapper;
    @Autowired
    VipServiceConfigMapper vipServiceConfigMapper;

    @Override
    public void processPayment(Long userId, String orderNo, List<Long> itemIdList) throws Exception {
        //根据用户id，订单号获取用户vip服务记录
        UserVipService userVipService = userVipServiceMapper.selectOne(new LambdaQueryWrapper<UserVipService>()
                .eq(UserVipService::getUserId, userId)
                .eq(UserVipService::getOrderNo, orderNo));
        if(userVipService!=null){
            //已经更新过，直接返回
            return;
        }

        userVipService = new UserVipService();

        //更新用户的过期时间
        //根据id获取vip服务配置信息
        VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(itemIdList.get(0));
        //根据id获取用户信息
        UserInfo userInfo = userInfoMapper.selectById(userId);
        //判断当前用户是否处于vip状态
        if(userInfo.getIsVip()==1 && userInfo.getVipExpireTime().after(new Date())){
            //设置vip服务记录的开始时间
            userVipService.setStartTime(userInfo.getVipExpireTime());
            //用户处于vip状态，续期
            userInfo.setVipExpireTime(new DateTime(userInfo.getVipExpireTime()).plusMonths(vipServiceConfig.getServiceMonth()).toDate());
            //设置vip服务记录的过期时间
            userVipService.setExpireTime(userInfo.getVipExpireTime());
        }else {
            //用户不是vip状态，设置vip过期时间
            userInfo.setVipExpireTime(new DateTime().plusMonths(vipServiceConfig.getServiceMonth()).toDate());
            //设置vip服务记录的开始时间和过期时间
            userVipService.setStartTime(new Date());
            userVipService.setExpireTime(userInfo.getVipExpireTime());
        }

        //更新vip服务记录和用户信息
        userInfo.setIsVip(1);
        userVipService.setOrderNo(orderNo);
        userVipService.setUserId(userId);
        userInfoMapper.updateById(userInfo);
        userVipServiceMapper.insert(userVipService);
    }
}
