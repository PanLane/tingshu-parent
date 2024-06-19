package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.annotation.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "用户声音播放进度管理接口")
@RestController
@RequestMapping("api/user/userListenProcess")
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserListenProcessApiController {

	@Autowired
	private UserListenProcessService userListenProcessService;

	@TsLogin(required = false)
	@Operation(summary = "获取声音播放进度")
	@GetMapping("/getTrackBreakSecond/{trackId}")
	public Result<BigDecimal> getTrackBreakSecond(@PathVariable Long trackId) {
		BigDecimal breakSecond = userListenProcessService.getTrackBreakSecond(trackId);
		return Result.ok(breakSecond);
	}

	@TsLogin(required = false)
	@Operation(summary = "更新用户播放进度")
	@PostMapping("/updateListenProcess")
	public Result<Void> updateListenProcess(@RequestBody UserListenProcessVo userListenProcessVo) {
		userListenProcessService.updateListenProcess(userListenProcessVo);
		return Result.ok();
	}

	@TsLogin(required = false)
	@Operation(summary = "获取用户最近一次播放记录")
	@GetMapping("/getLatelyTrack")
	public Result<Map<String,Object>> getLatelyTrack(){
		Map<String,Object> map = userListenProcessService.getLatelyTrack();
		return Result.ok(map);
	}
}

