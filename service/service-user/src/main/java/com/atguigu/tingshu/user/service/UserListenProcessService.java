package com.atguigu.tingshu.user.service;

import com.atguigu.tingshu.vo.user.UserListenProcessVo;

import java.math.BigDecimal;
import java.util.Map;

public interface UserListenProcessService {

    /**
     * 获取声音播放进度
     * @param trackId
     * @return
     */
    BigDecimal getTrackBreakSecond(Long trackId);

    /**
     * 更新声音播放进度
     * @param userListenProcessVo
     */
    void updateListenProcess(UserListenProcessVo userListenProcessVo);

    /**
     * 获取用户最近一次播放记录
     * @return
     */
    Map<String, Object> getLatelyTrack();
}
