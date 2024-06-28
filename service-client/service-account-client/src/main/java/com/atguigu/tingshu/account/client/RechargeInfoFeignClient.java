package com.atguigu.tingshu.account.client;

import com.atguigu.tingshu.account.client.impl.RechargeInfoDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.account.RechargeInfo;
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
@FeignClient(value = "service-account", fallback = RechargeInfoDegradeFeignClient.class,path = "api/account/rechargeInfo")
public interface RechargeInfoFeignClient {

    @GetMapping("/getByOrderNo/{orderNo}")
    Result<RechargeInfo> getByOrderNo(@PathVariable String orderNo);
}