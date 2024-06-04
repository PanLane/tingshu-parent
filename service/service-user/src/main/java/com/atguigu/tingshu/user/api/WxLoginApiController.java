package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.annotation.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "微信授权登录接口")
@RestController
@RequestMapping("/api/user/wxLogin")
@Slf4j
public class WxLoginApiController {

    @Autowired
    private UserInfoService userInfoService;

    @Operation(summary = "微信授权登录")
    @GetMapping("/wxLogin/{code}")
    public Result<Map<String,Object>> wxLogin(@PathVariable String code) throws WxErrorException {
        return Result.ok(userInfoService.wxLogin(code));
    }

    @TsLogin
    @Operation(summary = "获取用户信息")
    @GetMapping("/getUserInfo")
    public Result<UserInfoVo> getUserInfo() {
        return Result.ok(userInfoService.getUserInfo(AuthContextHolder.getUserId()));
    }

    @TsLogin
    @Operation(summary = "更新用户信息")
    @PostMapping("/updateUser")
    public Result<Void> updateUser(@RequestBody UserInfoVo userInfoVo) {
        userInfoVo.setId(AuthContextHolder.getUserId());
        userInfoService.updateUser(userInfoVo);
        return Result.ok();
    }
}
