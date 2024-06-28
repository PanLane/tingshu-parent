package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.annotation.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
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

	@TsLogin
	@Operation(summary = "判断用户是否购买过指定专辑")
	@GetMapping("/isPaidAlbum/{albumId}")
	public Result<Boolean> isPaidAlbum(@PathVariable Long albumId) {
		return Result.ok(userInfoService.isPaidAlbum(albumId));
	}

	@TsLogin
	@Operation(summary = "获取用户已购买声音id的集合")
	@GetMapping("/getPaidTrackIdList/{albumId}")
	Result<List<Long>> getPaidTrackIdList(@PathVariable Long albumId){
		return Result.ok(userInfoService.getPaidTrackIdList(albumId));
	}

	@Operation(summary = "更新用户vip状态")
	@GetMapping("/updateUserVipStatus")
	public Result<Void> updateUserVipStatus() {
		userInfoService.updateUserVipStatus();
		return Result.ok();
	}

	@Operation(summary = "更新支付成功后的状态")
	@PostMapping("/payRecord")
	public Result<Void> payRecord(@RequestBody UserPaidRecordVo userPaidRecordVo) throws Exception {
		userInfoService.updateUserPaidRecord(userPaidRecordVo);
		return Result.ok();
	}


}

