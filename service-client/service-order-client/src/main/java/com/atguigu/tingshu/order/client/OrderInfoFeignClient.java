package com.atguigu.tingshu.order.client;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.order.OrderInfo;
import com.atguigu.tingshu.order.client.impl.OrderInfoDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-order", fallback = OrderInfoDegradeFeignClient.class,path = "api/order/orderInfo")
public interface OrderInfoFeignClient {
    @GetMapping("/getOrderInfo/{orderNo}")
    Result<OrderInfo> getOrderInfo(@PathVariable String orderNo);
}