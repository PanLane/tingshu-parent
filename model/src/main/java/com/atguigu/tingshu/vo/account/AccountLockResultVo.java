package com.atguigu.tingshu.vo.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Schema(description = "锁定金额返回对象")
@AllArgsConstructor
@NoArgsConstructor
public class AccountLockResultVo {

	@Schema(description = "用户id")
	private Long userId;

	@Schema(description = "锁定金额")
	private BigDecimal amount;

	@Schema(description = "锁定内容")
	private String content;

}