package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;
import me.chanjar.weixin.common.error.WxErrorException;

import java.util.List;
import java.util.Map;

public interface UserInfoService extends IService<UserInfo> {


    Map<String, Object> wxLogin(String code) throws WxErrorException;

    /**
     * 获取用户信息
     * @param userId
     * @return
     */
    UserInfoVo getUserInfo(Long userId);

    /**
     * 更新用户信息
     * @param userInfoVo
     */
    void updateUser(UserInfoVo userInfoVo);

    /**
     * 获取需要付费的声音id集合
     * @param albumId
     * @param mayNeedPaidTrackIdList
     * @return
     */
    List<Long> getNotFreeTrackIdList(Long albumId, List<Long> mayNeedPaidTrackIdList);
}
