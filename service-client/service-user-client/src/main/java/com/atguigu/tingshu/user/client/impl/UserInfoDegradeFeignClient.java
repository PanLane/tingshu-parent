package com.atguigu.tingshu.user.client.impl;


import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
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
}
