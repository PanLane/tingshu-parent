package com.atguigu.tingshu.account.service;

import com.atguigu.tingshu.model.account.RechargeInfo;
import com.atguigu.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface RechargeInfoService extends IService<RechargeInfo> {

    /**
     * 提交充值
     * @param rechargeInfoVo
     * @return
     */
    String submitRecharge(RechargeInfoVo rechargeInfoVo);
}
