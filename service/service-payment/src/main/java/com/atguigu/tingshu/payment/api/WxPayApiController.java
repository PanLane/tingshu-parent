package com.atguigu.tingshu.payment.api;

import com.atguigu.tingshu.common.annotation.TsLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.payment.service.WxPayService;
import com.wechat.pay.java.service.payments.model.Transaction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "微信支付接口")
@RestController
@RequestMapping("api/payment/wxPay")
@Slf4j
public class WxPayApiController {

    @Autowired
    private WxPayService wxPayService;

    @TsLogin
    @Operation(summary = "微信支付")
    @PostMapping("/createJsapi/{paymentType}/{orderNo}")
    public Result<Map<String,Object>> createJsapi(@PathVariable String paymentType, @PathVariable String orderNo) {
        return Result.ok(wxPayService.createJsapi(paymentType,orderNo));
    }

    @Operation(summary = "手动查询订单状态")
    @GetMapping("/queryPayStatus/{orderNo}")
    public Result<Transaction> queryPayStatus(@PathVariable String orderNo) {
        Transaction result = wxPayService.queryPayStatus(orderNo);
        if(Transaction.TradeStateEnum.SUCCESS.equals(result.getTradeState())){
            //支付成功，更新交易记录状态
            wxPayService.updatePaymentStatus(orderNo);
            return Result.ok(result);
        }
        return Result.fail(result);
    }

    @Operation(summary = "微信支付异步回调")
    @PostMapping("/notify")
    public Result<Transaction> wxNotify(HttpServletRequest request){
        Transaction transaction = wxPayService.wxNotify(request);
        if(Transaction.TradeStateEnum.SUCCESS.equals(transaction.getTradeState())){
            //支付成功，更新交易记录状态
            wxPayService.updatePaymentStatus(transaction.getOutTradeNo());
            return Result.ok(transaction);
        }
        return Result.fail(transaction);
    }
}
