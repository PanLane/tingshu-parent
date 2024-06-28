package com.atguigu.tingshu.account.client;

import com.atguigu.tingshu.account.client.impl.UserAccountDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.vo.account.AccountLockResultVo;
import com.atguigu.tingshu.vo.account.AccountLockVo;
import com.atguigu.tingshu.vo.account.AccountMinusVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-account", fallback = UserAccountDegradeFeignClient.class,path = "api/account/userAccount")
public interface UserAccountFeignClient {

    @PostMapping("/checkAndLock")
    Result<AccountLockResultVo> checkAndLock(@RequestBody AccountLockVo accountLockVo);

    @PostMapping("/checkAndMinus")
    Result<AccountLockResultVo> checkAndMinus(@RequestBody AccountMinusVo accountMinusVo);

}