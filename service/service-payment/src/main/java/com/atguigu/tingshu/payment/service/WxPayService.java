package com.atguigu.tingshu.payment.service;


import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface WxPayService {

    /**
     * 微信支付
     * @param paymentType
     * @param orderNo
     */
    Map<String,Object> createJsapi(String paymentType, String orderNo);

    /**
     * 查询订单状态
     * @param orderNo
     * @return
     */
    Transaction queryPayStatus(String orderNo);

    /**
     * 更新交易状态
     * @param orderNo
     */
    void updatePaymentStatus(String orderNo);

    /**
     *
     * @param request
     * @return
     */
    Transaction wxNotify(HttpServletRequest request);
}
