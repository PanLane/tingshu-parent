package com.atguigu.tingshu.payment.service.impl;

import com.atguigu.tingshu.account.client.RechargeInfoFeignClient;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.model.payment.PaymentInfo;
import com.atguigu.tingshu.order.client.OrderInfoFeignClient;
import com.atguigu.tingshu.payment.config.WxPayV3Config;
import com.atguigu.tingshu.payment.mapper.PaymentInfoMapper;
import com.atguigu.tingshu.payment.service.PaymentInfoService;
import com.atguigu.tingshu.payment.service.WxPayService;
import com.atguigu.tingshu.payment.util.PayUtil;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.QueryOrderByOutTradeNoRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {

    @Autowired
    private PaymentInfoService paymentInfoService;
    @Autowired
    private WxPayV3Config wxPayV3Config;
    @Autowired
    private OrderInfoFeignClient orderInfoFeignClient;
    @Autowired
    private PaymentInfoMapper paymentInfoMapper;
    @Autowired
    private RSAAutoCertificateConfig rsaAutoCertificateConfig;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private UserInfoFeignClient userInfoFeignClient;
    @Autowired
    private RechargeInfoFeignClient rechargeInfoFeignClient;

    @Override
    public Map<String,Object> createJsapi(String paymentType, String orderNo) {

        //保存支付信息
        PaymentInfo paymentInfo = savePaymentInfo(orderNo, paymentType);
        if(paymentInfo == null){
            //保存失败，用户已取消订单，直接返回
            return null;
        }

        // 使用自动更新平台证书的RSA配置
        // 构建service
        JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(rsaAutoCertificateConfig).build();
        // request.setXxx(val)设置所需参数，具体参数可见Request定义
        PrepayRequest request = new PrepayRequest();
        Amount amount = new Amount();
        amount.setTotal(1);
        request.setAmount(amount);
        request.setAppid(wxPayV3Config.getAppid());
        request.setMchid(wxPayV3Config.getMerchantId());
        request.setDescription(paymentInfo.getContent());
        request.setNotifyUrl(wxPayV3Config.getNotifyUrl());
        request.setOutTradeNo(orderNo);
        Payer payer = new Payer();
        payer.setOpenid(userInfoFeignClient.getUserInfoById(paymentInfo.getUserId()).getData().getWxOpenId());
        request.setPayer(payer);
        // 调用下单方法，得到应答
        PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request);

        Map<String, Object> result = new HashMap<>();
        result.put("timeStamp", response.getTimeStamp()); // 时间戳
        result.put("nonceStr", response.getNonceStr());   // 随机字符串
        result.put("package", response.getPackageVal());  // 订单详情扩展字符串
        result.put("signType", response.getSignType());   // 签名方式
        result.put("paySign", response.getPaySign());     // 签名
        return result;
    }

    @Override
    public Transaction queryPayStatus(String orderNo) {
        QueryOrderByOutTradeNoRequest queryRequest = new QueryOrderByOutTradeNoRequest();
        queryRequest.setMchid(wxPayV3Config.getMerchantId());
        queryRequest.setOutTradeNo(orderNo);
        // 构建service
        NativePayService service = new NativePayService.Builder().config(rsaAutoCertificateConfig).build();
        return service.queryOrderByOutTradeNo(queryRequest);
    }

    @Override
    public void updatePaymentStatus(String orderNo) {
        //根据订单号查询支付信息
        Wrapper<PaymentInfo> wrapper = new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOutTradeNo, orderNo);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(wrapper);
        Assert.notNull(paymentInfo, "支付信息为空");
        if (SystemConstant.PAYMENT_STATUS_PAID.equals(paymentInfo.getPaymentStatus())) {
            //已更新，直接返回
            return;
        }
        paymentInfo.setPaymentStatus(SystemConstant.PAYMENT_STATUS_PAID);
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent("支付成功");
        //更新支付信息
        paymentInfoMapper.updateById(paymentInfo);

        //发送异步请求，更新用户购买信息
        if (SystemConstant.PAYMENT_TYPE_ORDER.equals(paymentInfo.getPaymentType())) {
            kafkaTemplate.send(KafkaConstant.QUEUE_ORDER_PAY_SUCCESS,orderNo);
        }else {
            kafkaTemplate.send(KafkaConstant.QUEUE_RECHARGE_PAY_SUCCESS,orderNo);
        }


    }

    @Override
    public Transaction wxNotify(HttpServletRequest request) {
        String requestBody = PayUtil.readData(request);
        // 构造 RequestParam
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(request.getHeader("wechatPaySerial"))
                .nonce(request.getHeader("wechatpayNonce"))
                .signature(request.getHeader("wechatSignature"))
                .timestamp(request.getHeader("wechatTimestamp"))
                .body(requestBody)
                .build();

        // 如果已经初始化了 RSAAutoCertificateConfig，可直接使用
        // 没有的话，则构造一个
        NotificationConfig config = new RSAAutoCertificateConfig.Builder()
                .merchantId(wxPayV3Config.getMerchantId())
                .privateKeyFromPath(wxPayV3Config.getPrivateKeyPath())
                .merchantSerialNumber(wxPayV3Config.getMerchantSerialNumber())
                .apiV3Key(wxPayV3Config.getApiV3key())
                .build();

        // 初始化 NotificationParser
        NotificationParser parser = new NotificationParser(config);

        // 以支付通知回调为例，验签、解密并转换成 Transaction
        return parser.parse(requestParam, Transaction.class);
    }

    private PaymentInfo savePaymentInfo(String orderNo, String paymentType) {
        PaymentInfo paymentInfo = new PaymentInfo();
        if(SystemConstant.PAYMENT_TYPE_ORDER.equals(paymentType)){
            //支付订单
            //调用订单服务，根据orderNo获取订单信息
            Result<OrderInfo> orderInfoResult = orderInfoFeignClient.getOrderInfo(orderNo);
            Assert.notNull(orderInfoResult, "订单信息结果集为空");
            OrderInfo orderInfo = orderInfoResult.getData();
            Assert.notNull(orderInfo, "订单信息为空");
            if (SystemConstant.ORDER_STATUS_CANCEL.equals(orderInfo.getOrderStatus())) {
                //订单已取消，返回null
                return null;
            }
            paymentInfo.setUserId(orderInfo.getUserId());
            paymentInfo.setAmount(orderInfo.getOrderAmount());
            paymentInfo.setContent(orderInfo.getOrderTitle());
        }else {
            //充值
            //调用充值微服务，根据orderNo获取充值信息
            Result<RechargeInfo> rechargeInfoResult = rechargeInfoFeignClient.getByOrderNo(orderNo);
            Assert.notNull(rechargeInfoResult, "充值信息结果集为空");
            RechargeInfo rechargeInfo = rechargeInfoResult.getData();
            Assert.notNull(rechargeInfo, "充值信息为空");
            paymentInfo.setUserId(rechargeInfo.getUserId());
            paymentInfo.setAmount(rechargeInfo.getRechargeAmount());
            paymentInfo.setContent(SystemConstant.PAYMENT_TYPE_RECHARGE+":"+rechargeInfo.getRechargeAmount());
        }

        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setOutTradeNo(orderNo);
        paymentInfo.setPayWay(SystemConstant.ORDER_PAY_WAY_WEIXIN);
        paymentInfo.setPaymentStatus(SystemConstant.PAYMENT_STATUS_UNPAID);
        paymentInfoMapper.insert(paymentInfo);

        return paymentInfo;
    }
}
