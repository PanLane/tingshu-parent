package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.RechargeInfoService;
import com.atguigu.tingshu.common.annotation.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "充值管理")
@RestController
@RequestMapping("api/account/rechargeInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class RechargeInfoApiController {

	@Autowired
	private RechargeInfoService rechargeInfoService;

	@TsLogin
	@Operation(summary = "充值")
	@PostMapping("/submitRecharge")
	public Result<Map<String,Object>> submitRecharge(@RequestBody RechargeInfoVo rechargeInfoVo) {
		String orderNo = rechargeInfoService.submitRecharge(rechargeInfoVo);
		HashMap<String, Object> map = new HashMap<>();
		map.put("orderNo",orderNo);
		return Result.ok(map);
	}

	@Operation(summary = "根据订单号获取充值信息")
	@GetMapping("/getByOrderNo/{orderNo}")
	public Result<RechargeInfo> getByOrderNo(@PathVariable String orderNo){
		LambdaQueryWrapper<RechargeInfo> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(RechargeInfo::getOrderNo,orderNo);
		return Result.ok(rechargeInfoService.getOne(wrapper));
	}
}

