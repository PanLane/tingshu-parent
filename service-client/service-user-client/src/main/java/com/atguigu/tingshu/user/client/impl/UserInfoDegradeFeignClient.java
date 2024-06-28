package com.atguigu.tingshu.user.client.impl;


import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserInfoDegradeFeignClient implements UserInfoFeignClient {

    @Override
    public Result<UserInfoVo> getUserInfoById(Long userId) {
        return null;
    }

    @Override
    public Result<List<Long>> getNotFreeTrackIdList(Long albumId, List<Long> mayNeedPaidTrackIdList) {
        return null;
    }

    @Override
    public Result<Boolean> isPaidAlbum(Long albumId) {
        return null;
    }

    @Override
    public Result<List<Long>> getPaidTrackIdList(Long albumId) {
        return null;
    }

    @Override
    public Result<Void> updateUserVipStatus() {
        return null;
    }

    @Override
    public Result<Void> payRecord(UserPaidRecordVo userPaidRecordVo) {
        return null;
    }
}
