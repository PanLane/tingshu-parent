package com.atguigu.tingshu.dispatch.job;

import com.atguigu.tingshu.dispatch.mapper.XxlJobLogMapper;
import com.atguigu.tingshu.model.dispatch.XxlJobLog;
import com.atguigu.tingshu.search.client.SearchFeignClient;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class DispatchJobHandler {

    @Autowired
    private SearchFeignClient searchFeignClient;
    @Autowired
    private XxlJobLogMapper xxlJobLogMapper;
    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    /**
     * 更新排行榜
     */
    @XxlJob("updateLatelyAlbumRanking")
    public void updateLatelyAlbumRanking() {
        long startTime = System.currentTimeMillis();
        XxlJobLog xxlJobLog = new XxlJobLog();
        try {
            xxlJobLog.setJobId(XxlJobHelper.getJobId());
            searchFeignClient.updateLatelyAlbumRanking();
            xxlJobLog.setStatus(1);
        } catch (IOException e) {
            xxlJobLog.setStatus(0);
            xxlJobLog.setError(e.getMessage());
            e.printStackTrace();
        }finally {
            long endTime = System.currentTimeMillis();
            xxlJobLog.setTimes((int) (endTime-startTime));
            xxlJobLogMapper.insert(xxlJobLog);
        }
    }

    /**
     * 更新用户会员状态
     */
    @XxlJob("updateUserVipStatus")
    public void updateUserVipStatus() {
        long startTime = System.currentTimeMillis();
        XxlJobLog xxlJobLog = new XxlJobLog();
        try {
            xxlJobLog.setJobId(XxlJobHelper.getJobId());
            userInfoFeignClient.updateUserVipStatus();
            xxlJobLog.setStatus(1);
        } catch (Exception e) {
            xxlJobLog.setStatus(0);
            xxlJobLog.setError(e.getMessage());
            e.printStackTrace();
        }finally {
            long endTime = System.currentTimeMillis();
            xxlJobLog.setTimes((int) (endTime-startTime));
            xxlJobLogMapper.insert(xxlJobLog);
        }
    }
}