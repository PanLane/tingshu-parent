package com.atguigu.tingshu.user.client;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.user.client.impl.UserInfoDegradeFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-user", fallback = UserInfoDegradeFeignClient.class,path = "/api/user")
public interface UserInfoFeignClient {

    @GetMapping("/wxLogin/getUserInfoById")
    Result<UserInfoVo> getUserInfoById(@RequestParam Long userId);

    @PostMapping("/userInfo/getNotFreeTrackIdList/{albumId}")
    Result<List<Long>> getNotFreeTrackIdList(@PathVariable Long albumId, @RequestBody List<Long> mayNeedPaidTrackIdList);
}