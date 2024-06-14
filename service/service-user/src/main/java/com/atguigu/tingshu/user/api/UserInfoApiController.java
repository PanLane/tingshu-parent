package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.annotation.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.user.service.UserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user/userInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserInfoApiController {

	@Autowired
	private UserInfoService userInfoService;

	@TsLogin
	@Operation(summary = "获取需要付费的声音id集合")
	@PostMapping("/getNotFreeTrackIdList/{albumId}")
	public Result<List<Long>> getNotFreeTrackIdList(@PathVariable Long albumId,@RequestBody List<Long> mayNeedPaidTrackIdList) {
		return Result.ok(userInfoService.getNotFreeTrackIdList(albumId,mayNeedPaidTrackIdList));
	}



}

