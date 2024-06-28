package com.atguigu.tingshu.order.api;

import com.atguigu.tingshu.common.annotation.TsLogin;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.atguigu.tingshu.vo.order.OrderDetailVo;
import com.atguigu.tingshu.vo.order.OrderInfoVo;
import com.atguigu.tingshu.vo.order.TradeResponseVo;
import com.atguigu.tingshu.vo.order.TradeVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "订单管理")
@RestController
@RequestMapping("api/order/orderInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoApiController {

	@Autowired
	private OrderInfoService orderInfoService;

	@TsLogin
	@Operation(summary = "订单详情")
	@PostMapping("/trade")
	public Result<OrderInfoVo> trade(@RequestBody TradeVo tradeVo) {
		return Result.ok(orderInfoService.trade(tradeVo));
	}

	@TsLogin
	@Operation(summary = "订单提交")
	@PostMapping("/submitOrder")
		public Result<Map<String,Object>> submitOrder(@RequestBody OrderInfoVo orderInfoVo) {
		return Result.ok(orderInfoService.submitOrder(orderInfoVo));
	}

	@Operation(summary = "根据订单号获取订单信息")
	@GetMapping("/getOrderInfo/{orderNo}")
	public Result<OrderInfo> getOrderInfo(@PathVariable String orderNo) {
		return Result.ok(orderInfoService.getOrderInfo(orderNo));
	}

	@TsLogin
	@Operation(summary = "分页条件查询用户订单信息")
	@GetMapping("/findUserPage/{pageNo}/{pageSize}")
	public Result<IPage<OrderInfo>> findUserPage(@PathVariable Long pageNo,
												 @PathVariable Long pageSize,String orderStatus) {
		return Result.ok(orderInfoService.findUserPage(new Page(pageNo,pageSize),orderStatus));
	}
}

