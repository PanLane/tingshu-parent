package com.atguigu.tingshu.account.api;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.annotation.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.atguigu.tingshu.vo.account.AccountMinusVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "用户账户管理")
@RestController
@RequestMapping("api/account/userAccount")
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserAccountApiController {

	@Autowired
	private UserAccountService userAccountService;

	@TsLogin
	@Operation(summary = "获取用户账户余额")
	@GetMapping("/getAvailableAmount")
	public Result<BigDecimal> getAvailableAmount() {
		UserAccount userAccount = userAccountService.getOne(new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, AuthContextHolder.getUserId()));
		return Result.ok(userAccount.getAvailableAmount());
	}

	@TsLogin
	@Operation(summary = "检查及锁定账户余额")
	@PostMapping("/checkAndLock")
	public Result<AccountLockResultVo> checkAndLock(@RequestBody AccountLockVo accountLockVo) {
		return Result.ok(userAccountService.checkAndLock(accountLockVo));
	}

	@TsLogin
	@Operation(summary = "检查及扣减账户余额")
	@PostMapping("/checkAndMinus")
	public Result<AccountLockResultVo> checkAndMinus(@RequestBody AccountMinusVo accountMinusVo) {
		return Result.ok(userAccountService.checkAndMinus(accountMinusVo));
	}

	@TsLogin
	@Operation(summary = "获取用户消费记录")
	@GetMapping("/findUserConsumePage/{pageNo}/{pageSize}")
	public Result<IPage<UserAccountDetail>> findUserConsumePage(@PathVariable Long pageNo,
																@PathVariable Long pageSize) {
		Page<UserAccountDetail> page = new Page<>(pageNo, pageSize);
		return Result.ok(userAccountService.findUserConsumePage(page));
	}

	@TsLogin
	@Operation(summary = "获取用户消费记录")
	@GetMapping("/findUserRechargePage/{pageNo}/{pageSize}")
	public Result<IPage<UserAccountDetail>> findUserRechargePage(@PathVariable Long pageNo,
																@PathVariable Long pageSize) {
		Page<UserAccountDetail> page = new Page<>(pageNo, pageSize);
		return Result.ok(userAccountService.findUserRechargePage(page));
	}
}

